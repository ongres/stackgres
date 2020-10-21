/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.resource.CustomResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StackGresReconciliator<T extends CustomResource> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresReconciliator.class);

  private CustomResourceScanner<T> clusterScanner;

  private Conciliator<T> clusterConciliator;

  private HandlerDelegator<T> handlerDelegator;

  public synchronized void reconcile() {
    clusterScanner.getResources().forEach(cluster -> {
      final ObjectMeta metadata = cluster.getMetadata();
      final String clusterId = metadata.getNamespace() + "/" + metadata.getName();
      LOGGER.info("Checking reconciliation status of cluster "
          + clusterId);
      try {
        ReconciliationResult result = clusterConciliator.evalReconciliationState(cluster);
        if (!result.isUpToDate()) {
          LOGGER.info("Cluster " + clusterId + " it's not up to date. Reconciling");

          result.getCreations()
              .forEach(resource -> {
                LOGGER.info("Creating resource " + resource.getMetadata().getName()
                    + " of kind: " + resource.getKind());
                try {
                  handlerDelegator.create(resource);
                } catch (KubernetesClientException ex) {
                  if (ex.getCode() == 409) {
                    handlerDelegator.patch(resource);
                  } else {
                    throw ex;
                  }
                }
              });

          result.getPatches()
              .forEach(resource -> {
                LOGGER.info("Patching resource " + resource.getMetadata().getName()
                    + " of kind: " + resource.getKind());
                handlerDelegator.patch(resource);
              });

          result.getDeletions()
              .forEach(resource -> {
                LOGGER.info("Deleting resource " + resource.getMetadata().getName()
                    + " of kind: " + resource.getKind());
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
        LOGGER.error("Reconciliation of cluster " + clusterId + " failed", e);
        onError(e, cluster);
      }
    });

  }

  public abstract void onPostReconciliation(T config);

  public abstract void onConfigCreated(T context);

  public abstract void onConfigUpdated(T context);

  public abstract void onError(Exception e, T context);

  @Inject
  public void setClusterScanner(CustomResourceScanner<T> clusterScanner) {
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
