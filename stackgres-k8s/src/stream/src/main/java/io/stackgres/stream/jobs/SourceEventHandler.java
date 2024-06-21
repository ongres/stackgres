/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.concurrent.CompletableFuture;

import io.debezium.engine.format.SerializationFormat;
import io.stackgres.common.crd.sgstream.StackGresStream;

public interface SourceEventHandler {

  public <T> CompletableFuture<Void> streamChangeEvents(
      StackGresStream stream,
      Class<? extends SerializationFormat<T>> format,
      TargetEventConsumer<T> eventConsumer);

}
