/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.watcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.stackgres.operator.controller.ClusterController;
import io.stackgres.operator.customresources.sgcluster.StackGresCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StackGresClusterWatcher implements Watcher<StackGresCluster> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresClusterWatcher.class);

  @Inject
  ClusterController operator;

  @Override
  public void eventReceived(Action action, StackGresCluster resource) {
    LOGGER.info("Received an event with action: <{}>", action);
    LOGGER.debug("Action on resource: {}", resource);
    switch (action) {
      case ADDED:
        operator.newStackGresCluster(resource);
        break;
      case DELETED:
        operator.deleteStackGresCluster(resource);
        break;
      case MODIFIED:
        operator.updateStackGresCluster(resource);
        break;
      default:
        throw new UnsupportedOperationException("Action not supported: " + action);
    }
  }

  @Override
  public void onClose(KubernetesClientException cause) {
    LOGGER.error("onClose was called, ", cause);
    Runtime.getRuntime().halt(1);
  }

}
