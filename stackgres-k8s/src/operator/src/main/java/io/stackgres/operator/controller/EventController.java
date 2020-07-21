/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.OperatorProperty;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class EventController {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventController.class);

  private final KubernetesClientFactory kubClientFactory;
  private final Random random = new Random();

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
          .inNamespace(OperatorProperty.OPERATOR_NAMESPACE.getString())
          .withName(OperatorProperty.OPERATOR_NAME.getString())
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
      LOGGER.warn("Can not send event {} ({}), involved object was null", reason, message);
      return;
    }
    final Instant now = Instant.now();
    final String namespace = involvedObject.getMetadata().getNamespace();
    client.events()
        .inNamespace(namespace)
        .withLabels(Optional.ofNullable(involvedObject.getMetadata().getLabels())
            .orElse(ImmutableMap.of()))
        .list()
        .getItems()
        .stream()
        .filter(event -> isSameEvent(event, reason, message, involvedObject))
        .findAny()
        .map(event -> patchEvent(event, now, client))
        .orElseGet(() -> createEvent(namespace, now,
            reason, message, involvedObject, client));
  }

  private String nextId() {
    return Long.toHexString(random.nextLong());
  }

  private boolean isSameEvent(Event event, EventReason reason, String message,
      HasMetadata involvedObject) {
    return Objects.equals(
        event.getInvolvedObject().getKind(),
        involvedObject.getKind())
        && Objects.equals(
            event.getInvolvedObject().getNamespace(),
            involvedObject.getMetadata().getNamespace())
        && Objects.equals(
            event.getInvolvedObject().getName(),
            involvedObject.getMetadata().getName())
        && Objects.equals(
            event.getInvolvedObject().getUid(),
            involvedObject.getMetadata().getUid())
        && Objects.equals(
            event.getReason(),
            reason.reason())
        && Objects.equals(
            event.getType(),
            reason.type())
        && Objects.equals(
            event.getMessage(),
            message);
  }

  private Event patchEvent(Event event, Instant now, KubernetesClient client) {
    event.setCount(event.getCount() + 1);
    event.setLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(now));
    return client.events()
        .inNamespace(event.getMetadata().getNamespace())
        .withName(event.getMetadata().getName())
        .patch(event);
  }

  private Event createEvent(String namespace, Instant now,
      EventReason reason, String message, HasMetadata involvedObject,
      KubernetesClient client) {
    final String id = nextId();
    final String name = involvedObject.getMetadata().getName() + "." + id;
    return client.events()
        .inNamespace(namespace)
        .create(new EventBuilder()
          .withNewMetadata()
          .withNamespace(namespace)
          .withName(name)
          .withLabels(involvedObject.getMetadata().getLabels())
          .endMetadata()
          .withType(reason.type())
          .withReason(reason.reason())
          .withMessage(message)
          .withCount(1)
          .withFirstTimestamp(DateTimeFormatter.ISO_INSTANT.format(now))
          .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(now))
          .withSource(new EventSourceBuilder()
              .withComponent(OperatorProperty.OPERATOR_NAME.getString())
              .build())
          .withInvolvedObject(ResourceUtil.getObjectReference(involvedObject))
          .build());
  }

}
