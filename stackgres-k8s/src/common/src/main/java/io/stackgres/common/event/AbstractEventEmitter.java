/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.EventSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.EventReason;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEventEmitter<T extends HasMetadata> implements EventEmitter<T> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Random random = new Random();

  private KubernetesClient client;

  @Override
  public void sendEvent(EventReason reason, String message, T involvedObject) {
    if (involvedObject == null) {
      log.warn("Can not send event {} ({}), involved object was null", reason, message);
      return;
    }
    final Instant now = Instant.now();
    final String namespace = involvedObject.getMetadata().getNamespace();

    Event ev = client.v1().events()
        .inNamespace(namespace)
        .withLabels(Optional.ofNullable(involvedObject.getMetadata().getLabels())
            .orElse(Map.of()))
        .list()
        .getItems()
        .stream()
        .filter(event -> isSameEvent(event, reason, message, involvedObject))
        .findAny()
        .map(event -> patchEvent(event, now, client))
        .orElseGet(() -> createEvent(namespace, now,
            reason, message, involvedObject, client));
    log.debug("Sending event {}", ev.getMessage());
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
            reason.type().type())
        && Objects.equals(
            event.getMessage(),
            message);
  }

  private Event patchEvent(Event event, Instant now, KubernetesClient client) {
    event.setCount(event.getCount() + 1);
    event.setLastTimestamp(now.toString());
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
            .withFirstTimestamp(now.toString())
            .withLastTimestamp(now.toString())
            .withSource(new EventSourceBuilder()
                .withComponent(reason.component())
                .build())
            .withInvolvedObject(ResourceUtil.getObjectReference(involvedObject))
            .build())
        .create();
  }

  @Inject
  public void setClient(KubernetesClient client) {
    this.client = client;
  }
}
