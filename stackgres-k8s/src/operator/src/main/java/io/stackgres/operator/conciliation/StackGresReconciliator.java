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
import io.stackgres.common.resource.CustomResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StackGresReconciliator<T extends CustomResource<?, ?>> {

  private static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.reconciliator");

  private static final String STACKGRES_IO_RECONCILIATION = "stackgres.io/reconciliation-pause";

  private CustomResourceScanner<T> clusterScanner;

  private Conciliator<T> clusterConciliator;

  private HandlerDelegator<T> handlerDelegator;

  public synchronized void reconcile() {
    getExistentSources().forEach(cluster -> {
      final ObjectMeta metadata = cluster.getMetadata();
      final String clusterId = metadata.getNamespace() + "/" + metadata.getName();

      try {
        onPreReconciliation(cluster);
        LOGGER.info("Checking reconciliation status of cluster {}", clusterId);
        ReconciliationResult result = clusterConciliator.evalReconciliationState(cluster);
        if (!result.isUpToDate()) {
          LOGGER.info("Cluster {} it's not up to date. Reconciling", clusterId);

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
                LOGGER.info("Patching resource {} of kind: {}", resource.v2.getMetadata().getName(),
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
            onConfigCreated(cluster);
          } else {
            onConfigUpdated(cluster);
          }
        } else {
          LOGGER.info("Cluster " + clusterId + " it's up to date");
        }

        onPostReconciliation(cluster);

      } catch (Exception e) {
        LOGGER.error("Reconciliation of cluster {} failed", clusterId, e);
        try {
          onError(e, cluster);
        } catch (Exception onErrorEx) {
          LOGGER.error("Failed of executing on error event of cluster {}", clusterId, onErrorEx);
        }
      }
    });

  }

  private Stream<T> getExistentSources() {
    return clusterScanner.getResources().stream()
        .filter(r -> Optional.ofNullable(r.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true));
  }

  public abstract void onPreReconciliation(T config);

  public abstract void onPostReconciliation(T config);

  public abstract void onConfigCreated(T context);

  public abstract void onConfigUpdated(T context);

  public abstract void onError(Exception e, T context);

  @Inject
  public synchronized void setClusterScanner(CustomResourceScanner<T> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

  @Inject
  public void setClusterConciliator(Conciliator<T> clusterConciliator) {
    this.clusterConciliator = clusterConciliator;
  }

  @Inject
  public void setHandlerDelegator(HandlerDelegator<T> handlerDelegator) {
    this.handlerDelegator = handlerDelegator;
  }

}
