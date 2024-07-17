/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.resource.EventEmitter;
import io.stackgres.operatorframework.resource.EventReason;
import io.stackgres.stream.app.StreamProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EventController extends EventEmitter {

  private final CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  public EventController(CustomResourceFinder<StackGresStream> streamFinder) {
    this.streamFinder = streamFinder;
  }

  /**
   * Send an event.
   */
  public void sendEvent(EventReason reason, String message, KubernetesClient client) {
    StackGresStream stream = streamFinder
        .findByNameAndNamespace(
            StreamProperty.STREAM_NAME.getString(),
            StreamProperty.STREAM_NAMESPACE.getString())
        .orElse(null);
    sendEvent(reason, message, stream, client);
  }

}
