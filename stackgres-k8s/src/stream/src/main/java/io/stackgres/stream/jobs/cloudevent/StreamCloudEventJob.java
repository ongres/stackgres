/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.cloudevent;

import java.util.concurrent.CompletableFuture;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.stream.jobs.StateHandler;
import io.stackgres.stream.jobs.StreamEventStateHandler;
import io.stackgres.stream.jobs.StreamExecutorService;
import io.stackgres.stream.jobs.StreamJob;
import io.stackgres.stream.jobs.StreamTargetOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StreamTargetOperation("CloudEvent")
public class StreamCloudEventJob implements StreamJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamCloudEventJob.class);

  @Inject
  CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  @StateHandler("CloudEvent")
  StreamEventStateHandler cloudEventStateHandler;

  @Inject
  StreamExecutorService executorService;

  @Override
  public CompletableFuture<Void> runJob(StackGresStream stream) {
    LOGGER.info("Starting streaming to CloudEvent for SGStream {}", stream.getMetadata().getName());

    return cloudEventStateHandler.sendEvents(stream)
        .whenComplete((ignored, ex) -> {
          if (ex != null) {
            reportFailure(stream, ex);
          }
        });
  }

  private void reportFailure(StackGresStream stream, Throwable ex) {
    String message = ex.getMessage();
    String streamName = stream.getMetadata().getName();
    String namespace = stream.getMetadata().getNamespace();

    streamFinder.findByNameAndNamespace(streamName, namespace)
        .ifPresent(savedStream -> {
          if (savedStream.getStatus() == null) {
            savedStream.setStatus(new StackGresStreamStatus());
          }

          savedStream.getStatus().setFailure(message);

          streamScheduler.update(savedStream);
        });
  }

}
