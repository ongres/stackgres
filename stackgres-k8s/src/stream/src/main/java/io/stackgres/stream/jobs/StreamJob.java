/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.concurrent.CompletableFuture;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamJob.class);

  @Inject
  CustomResourceScheduler<StackGresStream> streamScheduler;

  @Inject
  StreamExecutorService executorService;

  public CompletableFuture<Void> runJob(
      StackGresStream stream,
      SourceEventHandler sourceEventHandler,
      TargetEventHandler targetEventHandler) {
    LOGGER.info("Starting streaming from {} to {} for SGStream {}",
        stream.getSpec().getSource().getType(),
        stream.getSpec().getTarget().getType(),
        stream.getMetadata().getName());

    return targetEventHandler.sendEvents(stream, sourceEventHandler)
        .whenComplete((ignored, ex) -> {
          if (ex != null) {
            reportFailure(stream, ex);
          }
        });
  }

  private void reportFailure(StackGresStream stream, Throwable ex) {
    String message = ex.getMessage();

    streamScheduler.update(stream, currentStream -> {
      if (currentStream.getStatus() == null) {
        currentStream.setStatus(new StackGresStreamStatus());
      }

      currentStream.getStatus().setFailure(message);
    });
  }

}
