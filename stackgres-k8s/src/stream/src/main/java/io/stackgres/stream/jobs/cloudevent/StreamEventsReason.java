/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.cloudevent;

import io.stackgres.operatorframework.resource.EventReason;
import io.stackgres.stream.app.StreamProperty;

public enum StreamEventsReason implements EventReason {
  ;

  private final Type type;
  private final String reason;

  StreamEventsReason(Type type, String reason) {
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
