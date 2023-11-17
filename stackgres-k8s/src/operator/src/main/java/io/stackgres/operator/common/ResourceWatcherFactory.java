/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.WatcherException;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.AbstractResourceWatcherFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResourceWatcherFactory extends AbstractResourceWatcherFactory {

  private final ResourceFinder<Service> serviceFinder;
  private final EventEmitter<Service> eventEmitter;


  /**
   * Create a {@code StackGresClusterWatcherFactory} instance.
   */
  @Inject
  public ResourceWatcherFactory(
      ResourceFinder<Service> serviceFinder,
      EventEmitter<Service> eventEmitter) {
    super();
    this.serviceFinder = serviceFinder;
    this.eventEmitter = eventEmitter;
  }

  @Override
  public void onError(WatcherException cause) {
    Service operatorService = serviceFinder
        .findByNameAndNamespace(
            OperatorProperty.OPERATOR_NAME.getString(),
            OperatorProperty.OPERATOR_NAME.getString())
        .orElse(null);
    eventEmitter.sendEvent(OperatorEventReason.OPERATOR_ERROR,
        "Watcher was closed unexpectedly: " + (cause.getMessage() != null
            ? cause.getMessage()
            : "unknown reason"), operatorService);
  }

  @Override
  protected void onClose() {
    // nothing to do
  }

}
