/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.function.Supplier;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamEventEmitter {

  private final EventEmitter<StackGresStream> eventEmitter;
  private final CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  public StreamEventEmitter(
      EventEmitter<StackGresStream> eventEmitter,
      CustomResourceFinder<StackGresStream> streamFinder) {
    this.eventEmitter = eventEmitter;
    this.streamFinder = streamFinder;
  }

  public void streamStarted(String streamName, String namespace) {
    var stream = streamFinder.findByNameAndNamespace(streamName, namespace)
        .orElseThrow(streamNotFound(streamName, namespace));

    eventEmitter.sendEvent(StreamEvents.STREAM_STARTED,
        "Stream started", stream);
  }

  public void streamCompleted(String streamName, String namespace) {
    var stream = streamFinder.findByNameAndNamespace(streamName, namespace)
        .orElseThrow(streamNotFound(streamName, namespace));

    eventEmitter.sendEvent(StreamEvents.STREAM_COMPLETED,
        "Stream completed", stream);
  }

  public void streamFailed(String streamName, String namespace) {
    var stream = streamFinder.findByNameAndNamespace(streamName, namespace)
        .orElseThrow(streamNotFound(streamName, namespace));

    eventEmitter.sendEvent(StreamEvents.STREAM_FAILED,
        "Stream failed", stream);
  }

  public void streamTimedOut(String streamName, String namespace) {
    var stream = streamFinder.findByNameAndNamespace(streamName, namespace)
        .orElseThrow(streamNotFound(streamName, namespace));

    eventEmitter.sendEvent(StreamEvents.STREAM_TIMEOUT,
        "Stream timed out", stream);
  }

  private Supplier<RuntimeException> streamNotFound(String streamName, String namespace) {
    return () ->
        new IllegalArgumentException("Stream " + streamName + "not found in namespace " + namespace);
  }

}
