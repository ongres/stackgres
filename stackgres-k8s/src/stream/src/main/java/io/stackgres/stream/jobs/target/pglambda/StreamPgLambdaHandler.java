/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.pglambda;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.debezium.engine.format.CloudEvents;
import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetCloudEventHttp;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetPgLambdaKnative;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.stream.jobs.SourceEventHandler;
import io.stackgres.stream.jobs.StreamTargetOperation;
import io.stackgres.stream.jobs.target.cloudevent.StreamCloudEventHandler;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@StreamTargetOperation(StreamTargetType.PG_LAMBDA)
public class StreamPgLambdaHandler extends StreamCloudEventHandler {

  @Override
  public CompletableFuture<Void> sendEvents(StackGresStream stream, SourceEventHandler sourceEventHandler) {
    var http = Optional.of(stream.getSpec().getTarget().getPgLambda().getKnative())
        .map(StackGresStreamTargetPgLambdaKnative::getHttp);
    final URI baseUri = URI.create(
        http.map(StackGresStreamTargetCloudEventHttp::getUrl)
        .orElse(StreamUtil.pglambdaUrl(stream)));
    final RetryHandler handler = createHandler(baseUri, http);
    return sourceEventHandler.streamChangeEvents(stream, CloudEvents.class, handler);
  }

}

