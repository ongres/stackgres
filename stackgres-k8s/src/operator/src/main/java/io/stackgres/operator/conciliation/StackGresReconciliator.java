/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.CustomResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StackGresReconciliator<T extends CustomResource<?, ?>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      StackGresReconciliator.class.getPackage().getName());

  private static final String STACKGRES_IO_RECONCILIATION = StackGresContext
      .RECONCILIATION_PAUSE_KEY;

  private CustomResourceScanner<T> scanner;

  private Conciliator<T> conciliator;

  private HandlerDelegator<T> handlerDelegator;

  public synchronized void reconcile() {
    getExistentSources().forEach(customResource -> {
      final ObjectMeta metadata = customResource.getMetadata();
      final String customResourceId = customResource.getKind()
          + " " + metadata.getNamespace() + "/" + metadata.getName();

      try {
        onPreReconciliation(customResource);
        LOGGER.info("Checking reconciliation status of {}", customResourceId);
        ReconciliationResult result = conciliator
            .evalReconciliationState(customResource);
        if (!result.isUpToDate()) {
          LOGGER.info("{} it's not up to date. Reconciling", customResourceId);

          result.getCreations()
              .forEach(resource -> {
                LOGGER.info("Creating resource {} of kind: {}",
                    resource.getMetadata().getName(), resource.getKind());
                try {
                  handlerDelegator.create(resource);
                } catch (KubernetesClientException ex) {
                  if (ex.getCode() == 409) {
                    handlerDelegator.replace(resource);
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
                handlerDelegator.patch(resource.v1, resource.v2);
              });

          result.getDeletions()
              .forEach(resource -> {
                LOGGER.info("Deleting resource {} of kind: {}", resource.getMetadata().getName(),
                    resource.getKind());
                handlerDelegator.delete(resource);
              });
          if (result.getDeletions().size() == 0 && result.getPatches().size() == 0) {
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
