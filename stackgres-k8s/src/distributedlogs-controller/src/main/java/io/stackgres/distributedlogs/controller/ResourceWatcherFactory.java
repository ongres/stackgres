/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.WatcherException;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.distributedlogs.common.DistributedLogsControllerEventReason;
import io.stackgres.operatorframework.resource.AbstractResourceWatcherFactory;

@ApplicationScoped
public class ResourceWatcherFactory extends AbstractResourceWatcherFactory {

  private final EventEmitter<StackGresDistributedLogs> eventEmitter;
  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  /**
   * Create a {@code DistributedLogsWatcherFactory} instance.
   */
  @Inject
  public ResourceWatcherFactory(
      EventEmitter<StackGresDistributedLogs> eventEmitter,
      CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder) {
    super();
    this.eventEmitter = eventEmitter;
    this.distributedLogsFinder = distributedLogsFinder;
  }

  @Override
  public void onError(WatcherException cause) {

    StackGresDistributedLogs distributedLogs = distributedLogsFinder
        .findByNameAndNamespace(
            DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME.getString(),
            DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE.getString())
        .orElse(null);

    eventEmitter.sendEvent(DistributedLogsControllerEventReason.DISTRIBUTEDLOGS_CONTROLLER_ERROR,
        "Watcher was closed unexpectedly: " + (cause.getMessage() != null
            ? cause.getMessage()
            : "unknown reason"), distributedLogs);
  }

  @Override
  protected void onClose() {
    // nothing to do
  }

}
