/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum StreamEventReason implements OperatorEventReason {

  STREAM_CREATED(NORMAL, "StreamCreated"),
  STREAM_UPDATED(NORMAL, "StreamUpdated"),
  STREAM_DELETED(NORMAL, "StreamDeleted"),
  STREAM_CONFIG_ERROR(WARNING, "StreamConfigFailed"),
  STREAM_ERROR(WARNING, "StreamFailed");

  private final Type type;
  private final String reason;

  StreamEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
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
