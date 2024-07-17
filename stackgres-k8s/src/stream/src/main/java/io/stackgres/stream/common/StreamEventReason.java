/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.common;

import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.StackGresContainer;
import io.stackgres.operatorframework.resource.EventReason;

public enum StreamEventReason implements EventReason {

  STREAM_ERROR(WARNING, "StreamFailed");

  private final Type type;
  private final String reason;

  StreamEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return StackGresContainer.STREAM_CONTROLLER.getName();
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
