/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.watcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.resource.SgReplicaSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresClusterWatcher implements Watcher<StackGresCluster> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresClusterWatcher.class);

  @Inject
  SgReplicaSets sgReplicas;

  @Override
  public void eventReceived(Action action, StackGresCluster resource) {
    LOGGER.info("Received an event with action : {} and the following resource : {}",
        action, resource);
    String clusterName = resource.getMetadata().getName();
    switch (action) {
      case ADDED:
        LOGGER.info("Starting the cluster with name '{}'", clusterName);
        for (int i = 0; i < resource.getSpec().getInstances(); i++) {
          ReplicaSet rs = sgReplicas.create(resource);
          LOGGER.info("ReplicaSet created '{}'", rs);
        }
        break;
      case DELETED:
        LOGGER.info("Deleting the cluster with name '{}'", clusterName);
        break;
      case MODIFIED:
        LOGGER.info("Modify the cluster with name '{}'", clusterName);
        break;
      case ERROR:
        LOGGER.info("Error on the cluster with name '{}'", clusterName);
        break;
      default:
        break;
    }
  }

  @Override
  public void onClose(KubernetesClientException cause) {
    LOGGER.error("onClose was called, ", cause);
  }

}
