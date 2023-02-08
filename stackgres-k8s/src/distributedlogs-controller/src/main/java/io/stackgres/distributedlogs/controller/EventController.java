/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.resource.EventEmitter;
import io.stackgres.operatorframework.resource.EventReason;

@ApplicationScoped
public class EventController extends EventEmitter {

  private final CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder;

  @Inject
  public EventController(CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder) {
    this.distributedLogsFinder = distributedLogsFinder;
  }

  /**
   * Send an event.
   */
  public void sendEvent(EventReason reason, String message, KubernetesClient client) {
    StackGresDistributedLogs distributedLogs = distributedLogsFinder
        .findByNameAndNamespace(
            DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME.getString(),
            DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE.getString())
        .orElse(null);
    sendEvent(reason, message, distributedLogs, client);
  }

}
