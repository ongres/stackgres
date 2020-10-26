/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.EventEmitter;
import io.stackgres.operatorframework.resource.EventReason;

@ApplicationScoped
public class EventController extends EventEmitter {

  private final ResourceFinder<Service> serviceFinder;

  @Inject
  public EventController(ResourceFinder<Service> serviceFinder) {
    this.serviceFinder = serviceFinder;
  }

  public EventController() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.serviceFinder = null;
  }

  /**
   * Send an event.
   */
  public void sendEvent(EventReason reason, String message, KubernetesClient client) {
    Service operatorService = serviceFinder
        .findByNameAndNamespace(
            OperatorProperty.OPERATOR_NAME.getString(),
            OperatorProperty.OPERATOR_NAME.getString())
        .orElse(null);
    sendEvent(reason, message, operatorService, client);
  }

}
