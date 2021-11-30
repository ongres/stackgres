/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.resource.CustomResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StackGresReconciliator<T extends CustomResource<?, ?>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      StackGresReconciliator.class.getPackage().getName());

  private static final String STACKGRES_IO_RECONCILIATION = StackGresContext
      .RECONCILIATION_PAUSE_KEY;

  private CustomResourceScanner<T> scanner;

  private Conciliator<T> conciliator;

  private HandlerDelegator<T> handlerDelegator;

  private final ExecutorService executorService;
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  public StackGresReconciliator() {
    this.executorService = Executors.newSingleThreadExecutor(
        r -> new Thread(r, getReconciliationName() + "-ReconciliationLoop"));
  }

  protected void start() {
    executorService.execute(this::reconciliationLoop);
  }

  protected void stop() {
    close = true;
    reconcile();
    executorService.shutdown();
    reconcile();
    stopped.join();
  }

  protected abstract String getReconciliationName();

  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
      justification = "We do not care if queue is already filled")
  public void reconcile() {
    arrayBlockingQueue.offer(Boolean.TRUE);
  }

  private void reconciliationLoop() {
    LOGGER.info("{} reconciliation loop started", getReconciliationName());
    while (true) {
      try {
        arrayBlockingQueue.take();
        if (close) {
          break;
        }
        reconciliationCycle();
      } catch (Exception ex) {
        LOGGER.error("{} reconciliation loop was interrupted", getReconciliationName(), ex);
      }
    }
    LOGGER.info("{} reconciliation loop stopped", getReconciliationName());
    stopped.complete(null);
  }

  public synchronized void reconciliationCycle() {
    getExistentSources().forEach(customResource -> {
      final ObjectMeta metadata = customResource.getMetadata();
      final String customResourceId = customResource.getKind()
          + " " + metadata.getNamespace() + "/" + metadata.getName();

      try {
        onPreReconciliation(customResource);
        LOGGER.info("Checking reconciliation status of {}", customResourceId);
        ReconciliationResult result = conciliator.evalReconciliationState(customResource);
        if (!result.isUpToDate()) {
          LOGGER.info("{} it's not up to date. Reconciling", customResourceId);

          result.getCreations()
              .forEach(resource -> {
                LOGGER.info("Creating resource {} of kind: {}",
                    resource.getMetadata().getName(), resource.getKind());
                try {
                  handlerDelegator.create(customResource, resource);
                } catch (KubernetesClientException ex) {
                  if (ex.getCode() == 409) {
                    handlerDelegator.replace(customResource, resource);
                  } else {
                    throw ex;
                  }
                }
              });

          result.getPatches()
              .forEach(resource -> {
                LOGGER.info("Patching resource {} of kind: {}",
                    resource.v2.getMetadata().getName(),
                    resource.v2.getKind());
                HasMetadata patchedResource = handlerDelegator
                    .patch(customResource, resource.v1, resource.v2);
                if (Optional.ofNullable(patchedResource.getMetadata().getManagedFields())
                    .stream()
                    .flatMap(List::stream)
                    .anyMatch(managedFieldsEntry -> Objects.equals(
                        managedFieldsEntry.getManager(), "before-first-apply"))) {
                  LOGGER.info("Owning resource {} of kind: {}",
                      resource.v2.getMetadata().getName(),
                      resource.v2.getKind());
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Before owning the resource was:\n{}",
                        StackGresUtil.toPrettyYaml(resource.v2));
                    LOGGER.debug("After patching the resource was:\n{}",
                        StackGresUtil.toPrettyYaml(patchedResource));
                  }
                  HasMetadata ownedResourceWithBeforeFirstApply = handlerDelegator
                      .patch(customResource, resource.v2, patchedResource);
                  Optional.ofNullable(ownedResourceWithBeforeFirstApply.getMetadata())
                      .map(ObjectMeta::getManagedFields)
                      .stream()
                      .flatMap(List::stream)
                      .filter(managedFieldsEntry -> Objects.equals(
                          managedFieldsEntry.getManager(), "before-first-apply"))
                      .collect(Collectors.toList())
                      .forEach(ownedResourceWithBeforeFirstApply
                          .getMetadata().getManagedFields()::remove);
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("To own the resource has been changed to:\n{}",
                        StackGresUtil.toPrettyYaml(ownedResourceWithBeforeFirstApply));
                  }
                  HasMetadata ownedResourceWithoutBeforeFirstApply = handlerDelegator.replace(
                      customResource, ownedResourceWithBeforeFirstApply);
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("After removing before-first-apply the resource is:\n{}",
                        StackGresUtil.toPrettyYaml(ownedResourceWithoutBeforeFirstApply));
                  }
                  HasMetadata ownedAndPatchedResource = handlerDelegator
                      .patch(customResource, resource.v1, ownedResourceWithoutBeforeFirstApply);
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("After owning the resource is:\n{}",
                        StackGresUtil.toPrettyYaml(ownedAndPatchedResource));
                  }
                }
              });

          result.getDeletions()
              .forEach(resource -> {
                LOGGER.info("Deleting resource {} of kind: {}", resource.getMetadata().getName(),
                    resource.getKind());
                handlerDelegator.delete(customResource, resource);
              });
          if (result.getDeletions().isEmpty() && result.getPatches().isEmpty()) {
            onConfigCreated(customResource, result);
          } else {
            onConfigUpdated(customResource, result);
          }
        } else {
          LOGGER.info("{} it's up to date", customResourceId);
        }

        onPostReconciliation(customResource);

      } catch (Exception e) {
        LOGGER.error("Reconciliation of {} failed", customResourceId, e);
        try {
          onError(e, customResource);
        } catch (Exception onErrorEx) {
          LOGGER.error("Failed executing on error event of {}", customResourceId, onErrorEx);
        }
      }
    });
  }

  private Stream<T> getExistentSources() {
    try {
      return scanner.getResources().stream()
          .filter(r -> Optional.ofNullable(r.getMetadata().getAnnotations())
              .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
              .map(Boolean::parseBoolean)
              .map(b -> !b)
              .orElse(true));
    } catch (Exception ex) {
      LOGGER.error("Failed retrieving existing sources", ex);
      return Stream.of();
    }
  }

  public abstract void onPreReconciliation(T config);

  public abstract void onPostReconciliation(T config);

  public abstract void onConfigCreated(T context, ReconciliationResult result);

  public abstract void onConfigUpdated(T context, ReconciliationResult result);

  public abstract void onError(Exception e, T context);

  @Inject
  public void setScanner(CustomResourceScanner<T> scanner) {
    this.scanner = scanner;
  }

  @Inject
  public void setConciliator(Conciliator<T> conciliator) {
    this.conciliator = conciliator;
  }

  @Inject
  public void setHandlerDelegator(HandlerDelegator<T> handlerDelegator) {
    this.handlerDelegator = handlerDelegator;
  }

}
