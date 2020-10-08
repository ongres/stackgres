/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operatorframework.resource.EventEmitter;
import io.stackgres.operatorframework.resource.EventReason;

@ApplicationScoped
public class EventController extends EventEmitter {

  /**
   * Send an event.
   */
  public void sendEvent(EventReason reason, String message, KubernetesClient client) {
    sendEvent(reason, message, client.services()
        .inNamespace(OperatorProperty.OPERATOR_NAMESPACE.getString())
        .withName(OperatorProperty.OPERATOR_NAME.getString())
        .get(), client);
  }

}
