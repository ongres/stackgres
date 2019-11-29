/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.time.Instant;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.resource.ResourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EventController {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventController.class);

  private final KubernetesClientFactory kubClientFactory;

  @Inject
  public EventController(KubernetesClientFactory kubClientFactory) {
    super();
    this.kubClientFactory = kubClientFactory;
  }

  /**
   * Send an event.
   */
  public void sendEvent(EventReason reason, String message) {
    try (KubernetesClient client = kubClientFactory.create()) {
      sendEvent(reason, message, client.services()
          .inNamespace(StackGresUtil.OPERATOR_NAMESPACE)
          .withName(StackGresUtil.OPERATOR_NAME)
          .get(), client);
    }
  }

  /**
   * Send an event related to a resource.
   */
  public void sendEvent(EventReason reason, String message, HasMetadata involvedObject) {
    try (KubernetesClient client = kubClientFactory.create()) {
      sendEvent(reason, message, involvedObject, client);
    }
  }

  private void sendEvent(EventReason reason, String message, HasMetadata involvedObject,
      KubernetesClient client) {
    if (involvedObject == null) {
      LOGGER.warn("Can not send event, involved object was null");
      return;
    }
    Instant now = Instant.now();
    final Long id = new Random().nextLong();
    client.resource(new EventBuilder()
        .withNewMetadata()
        .withName(involvedObject.getMetadata().getName() + "." + Long.toHexString(id))
        .withNamespace(involvedObject.getMetadata().getNamespace())
        .withLabels(involvedObject.getMetadata().getLabels())
        .endMetadata()
        .withFirstTimestamp(now.toString())
        .withLastTimestamp(now.toString())
        .withMessage(message)
        .withReason(reason.reason())
        .withSource(new EventSourceBuilder()
            .withComponent(StackGresUtil.OPERATOR_NAME)
            .build())
        .withInvolvedObject(ResourceUtil.getObjectReference(involvedObject))
        .build())
        .createOrReplace();
  }

}
