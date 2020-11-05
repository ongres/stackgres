/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.WatcherException;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.operatorframework.resource.AbstractResourceWatcherFactory;

@ApplicationScoped
public class ResourceWatcherFactory extends AbstractResourceWatcherFactory {

  private final KubernetesClientFactory clientFactory;
  private final EventController eventController;

  /**
   * Create a {@code StackGresClusterWatcherFactory} instance.
   */
  @Inject
  public ResourceWatcherFactory(
      KubernetesClientFactory clientFactory,
      EventController eventController) {
    super();
    this.clientFactory = clientFactory;
    this.eventController = eventController;
  }

  @Override
  public void onError(WatcherException cause) {
    try (KubernetesClient client = clientFactory.create()) {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
          "Watcher was closed unexpectedly: " + (cause.getMessage() != null
              ? cause.getMessage()
              : "unknown reason"), client);
    }
  }

  @Override
  protected void onClose() {
  }

}
