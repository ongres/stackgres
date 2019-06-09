/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.stackgres.crd.StackGresCluster;

public class StackGresClusterWatcher implements Watcher<StackGresCluster> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresClusterWatcher.class);

  @Override
  public void eventReceived(Action action, StackGresCluster resource) {
    LOGGER.info("Received an event with action : {} and the following resource : {}",
        action, resource);
    String clusterName = resource.getMetadata().getName();
    switch (action) {
      case ADDED:
        LOGGER.info("Starting the cluster with name '{}'", clusterName);
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
