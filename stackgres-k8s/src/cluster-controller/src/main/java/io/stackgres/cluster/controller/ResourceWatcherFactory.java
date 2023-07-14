/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.WatcherException;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.operatorframework.resource.AbstractResourceWatcherFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResourceWatcherFactory extends AbstractResourceWatcherFactory {

  private final KubernetesClient client;
  private final EventController eventController;

  /**
   * Create a {@code StackGresClusterWatcherFactory} instance.
   */
  @Inject
  public ResourceWatcherFactory(
      KubernetesClient client,
      EventController eventController) {
    super();
    this.client = client;
    this.eventController = eventController;
  }

  @Override
  public void onError(WatcherException cause) {
    eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
        "Watcher was closed unexpectedly: " + (cause.getMessage() != null
            ? cause.getMessage()
            : "unknown reason"),
        client);
  }

  @Override
  protected void onClose() {}

}
