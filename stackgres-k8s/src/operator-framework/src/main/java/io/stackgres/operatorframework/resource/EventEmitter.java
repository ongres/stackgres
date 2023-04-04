/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventEmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventEmitter.class);

  private final Random random = new Random();

  /**
   * Send an event related to a resource.
   */
  public void sendEvent(EventReason reason, String message, HasMetadata involvedObject,
                        KubernetesClient client) {
    if (involvedObject == null) {
      LOGGER.warn("Can not send event {} ({}), involved object was null", reason, message);
      return;
    }
    final Instant now = Instant.now();
    final String namespace = involvedObject.getMetadata().getNamespace();
    client.v1().events()
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
    return Long.toHexString(System.currentTimeMillis()) + Long.toHexString(random.nextLong());
  }

  private boolean isSameEvent(Event event, EventReason reason, String message,
      HasMetadata involvedObject) {
    return Objects.equals(
        event.getInvolvedObject().getKind(),
        involvedObject.getKind())
        && Objects.equals(
            event.getInvolvedObject().getUid(),
            involvedObject.getMetadata().getUid())
        && Objects.equals(
            event.getType(),
            reason.type().type())
        && Objects.equals(
            event.getReason(),
            reason.reason())
        && Objects.equals(
            event.getMessage(),
            message);
  }

  private Event patchEvent(Event event, Instant now, KubernetesClient client) {
    event.setCount(event.getCount() + 1);
    event.setLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(now));
    return client.v1().events()
        .inNamespace(event.getMetadata().getNamespace())
        .withName(event.getMetadata().getName())
        .patch(event);
  }

  private Event createEvent(String namespace, Instant now,
      EventReason reason, String message, HasMetadata involvedObject,
      KubernetesClient client) {
    final String id = nextId();
    final String name = involvedObject.getMetadata().getName() + "." + id;
    return client.v1().events()
        .inNamespace(namespace)
        .resource(new EventBuilder()
          .withNewMetadata()
          .withNamespace(namespace)
          .withName(name)
          .withLabels(involvedObject.getMetadata().getLabels())
          .endMetadata()
          .withType(reason.type().type())
          .withReason(reason.reason())
          .withMessage(message)
          .withCount(1)
          .withFirstTimestamp(DateTimeFormatter.ISO_INSTANT.format(now))
          .withLastTimestamp(DateTimeFormatter.ISO_INSTANT.format(now))
          .withSource(new EventSourceBuilder()
              .withComponent(reason.component())
              .build())
          .withInvolvedObject(ResourceUtil.getObjectReference(involvedObject))
          .build())
        .create();
  }

}
