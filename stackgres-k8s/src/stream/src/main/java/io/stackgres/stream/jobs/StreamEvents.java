/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import io.stackgres.operatorframework.resource.EventReason;
import io.stackgres.stream.app.StreamProperty;

public enum StreamEvents implements EventReason {
  STREAM_STARTED(Type.NORMAL, "StreamStarted"),
  STREAM_TIMEOUT(Type.WARNING, "StreamTimeOut"),
  STREAM_FAILED(Type.WARNING, "StreamFailed"),
  STREAM_COMPLETED(Type.NORMAL, "StreamCompleted");

  private final Type type;
  private final String reason;

  StreamEvents(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return StreamProperty.STREAM_NAME.getString();
  }

  @Override
  public String reason() {
    return reason;
  }

  @Override
  public Type type() {
    return type;
  }
}
