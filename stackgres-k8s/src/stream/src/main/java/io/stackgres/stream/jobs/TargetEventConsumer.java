/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import io.debezium.engine.ChangeEvent;

public interface TargetEventConsumer<T> extends AutoCloseable {

  void consumeEvent(ChangeEvent<T, T> event);

  @Override
  default void close() throws Exception {
  }

}
