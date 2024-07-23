/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.List;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine.RecordCommitter;

public interface TargetEventConsumer<T> extends AutoCloseable {

  void consumeEvents(List<ChangeEvent<T, T>> events, RecordCommitter<ChangeEvent<T, T>> committer);

  @Override
  default void close() throws Exception {
  }

}
