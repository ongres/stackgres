/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.distributedlogs.common.DistributedLogsEventReason;
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
  public void onError(KubernetesClientException cause) {
    try (KubernetesClient client = clientFactory.create()) {
      eventController.sendEvent(DistributedLogsEventReason.DISTRIBUTED_LOGS_CONTROLLER_ERROR,
          "Watcher was closed unexpectedly: " + (cause.getMessage() != null
              ? cause.getMessage()
              : "unknown reason"), client);
    }
  }

  @Override
  protected void onClose() {
  }

}
