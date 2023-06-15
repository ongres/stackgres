/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.CustomResourceScanner;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliator<T extends CustomResource<?, ?>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractReconciliator.class.getPackage().getName());

  private static final String STACKGRES_IO_RECONCILIATION = StackGresContext
      .RECONCILIATION_PAUSE_KEY;

  private final CustomResourceScanner<T> scanner;
  private final Conciliator<T> conciliator;
  private final HandlerDelegator<T> handlerDelegator;
  private final KubernetesClient client;
  private final String reconciliationName;
  private final ExecutorService executorService;
  private final AtomicReference<List<T>> atomicReference = new AtomicReference<List<T>>(List.of());
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  protected AbstractReconciliator(
      CustomResourceScanner<T> scanner,
      Conciliator<T> conciliator,
      HandlerDelegator<T> handlerDelegator,
      KubernetesClient client,
      String reconciliationName) {
    this.scanner = scanner;
    this.conciliator = conciliator;
    this.handlerDelegator = handlerDelegator;
    this.client = client;
    this.reconciliationName = reconciliationName;
    this.executorService = Executors.newSingleThreadExecutor(
        r -> new Thread(r, reconciliationName + "-ReconciliationLoop"));
  }

  public AbstractReconciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.scanner = null;
    this.conciliator = null;
    this.handlerDelegator = null;
    this.client = null;
    this.reconciliationName = null;
    this.executorService = null;
  }

  protected void start() {
    executorService.execute(this::reconciliationLoop);
  }

  protected void stop() {
    close = true;
    reconcile(List.of());
    executorService.shutdown();
    reconcile(List.of());
    stopped.join();
  }

  protected String getReconciliationName() {
    return reconciliationName;
  }

  public void reconcileAll() {
    reconcile(getExistentSources());
  }

  public void reconcile(T config) {
    reconcile(List.of(config));
  }

  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
      justification = "We do not care if queue is already filled")
  public void reconcile(List<T> configs) {
    atomicReference.updateAndGet(atomicConfigs -> Seq
        .seq(atomicConfigs)
        .filter(atomicConfig -> configs.stream()
            .anyMatch(config -> sameConfig(atomicConfig, config)))
        .append(configs)
        .toList());
    arrayBlockingQueue.offer(true);
  }

  private boolean sameConfig(T foundConfig, T newConfig) {
    return Objects.equals(
            foundConfig.getMetadata().getNamespace(),
            newConfig.getMetadata().getNamespace())
        && Objects.equals(
            foundConfig.getMetadata().getName(),
            newConfig.getMetadata().getName());
  }

  private List<T> getExistentSources() {
    try {
      return scanner.getResources();
    } catch (Exception ex) {
      LOGGER.error("Failed retrieving existing sources", ex);
      return List.of();
    }
  }

  private void reconciliationLoop() {
    LOGGER.info("{} reconciliation loop started", getReconciliationName());
    while (true) {
      try {
        arrayBlockingQueue.take();
        List<T> configs = atomicReference.getAndSet(List.of());
        if (close) {
          break;
        }
        reconciliationCycle(configs);
      } catch (Exception ex) {
        LOGGER.error("{} reconciliation loop was interrupted", getReconciliationName(), ex);
      }
    }
    LOGGER.info("{} reconciliation loop stopped", getReconciliationName());
    stopped.complete(null);
  }

  protected void reconciliationCycle(List<T> configs) {
    configs.stream()
        .filter(r -> Optional.ofNullable(r.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true))
        .forEach(this::reconciliationCycle);
  }

  private void reconciliationCycle(T config) {
    final ObjectMeta metadata = config.getMetadata();
    final String configId = config.getKind()
        + " " + metadata.getNamespace() + "/" + metadata.getName();

    try {
      onPreReconciliation(config);
      LOGGER.info("Checking reconciliation status of {}", configId);
      ReconciliationResult result = conciliator.evalReconciliationState(config);
      if (!result.isUpToDate()) {
        LOGGER.info("{} it's not up to date. Reconciling", configId);

        result.getCreations()
            .stream()
            .sorted(ReconciliationOperations.RESOURCES_COMPARATOR)
            .forEach(resource -> {
              LOGGER.info("Creating resource {}.{} of kind: {}",
                  resource.getMetadata().getNamespace(),
                  resource.getMetadata().getName(),
                  resource.getKind());
              handlerDelegator.create(config, resource);
            });

        result.getPatches()
            .stream()
            .sorted(Comparator.comparing(
                Tuple2::v1, ReconciliationOperations.RESOURCES_COMPARATOR))
            .forEach(resource -> {
              LOGGER.info("Patching resource {}.{} of kind: {}",
                  resource.v2.getMetadata().getNamespace(),
                  resource.v2.getMetadata().getName(),
                  resource.v2.getKind());
              handlerDelegator.patch(config, resource.v1, resource.v2);
            });

        result.getDeletions()
            .stream()
            .sorted(Collections.reverseOrder(
                ReconciliationOperations.RESOURCES_COMPARATOR))
            .forEach(resource -> {
              LOGGER.info("Deleting resource {}.{} of kind: {}",
                  resource.getMetadata().getNamespace(),
                  resource.getMetadata().getName(),
                  resource.getKind());
              handlerDelegator.delete(config, resource);
            });
        if (result.getDeletions().isEmpty() && result.getPatches().isEmpty()) {
          onConfigCreated(config, result);
        } else {
          onConfigUpdated(config, result);
        }
      } else {
        LOGGER.info("{} it's up to date", configId);
      }

      onPostReconciliation(config);

    } catch (Exception e) {
      LOGGER.error("Reconciliation of {} failed", configId, e);
      try {
        onError(e, config);
      } catch (Exception onErrorEx) {
        LOGGER.error("Failed executing on error event of {}", configId, onErrorEx);
      }
    }
  }

  protected abstract void onPreReconciliation(T config);

  protected abstract void onPostReconciliation(T config);

  protected abstract void onConfigCreated(T context, ReconciliationResult result);

  protected abstract void onConfigUpdated(T context, ReconciliationResult result);

  protected abstract void onError(Exception e, T context);

  public KubernetesClient getClient() {
    return client;
  }

}
