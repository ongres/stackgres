/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum DbOpsEventReason implements OperatorEventReason {

  DB_OPS_CREATED(NORMAL, "DbOpsCreated"),
  DB_OPS_FAILED(WARNING, "DbOpsFailed"),
  DB_OPS_TIMED_OUT(WARNING, "DbOpsTimedOut"),
  DB_OPS_COMPLETED(NORMAL, "DbOpsCompleted"),
  DB_OPS_CONFIG_ERROR(WARNING, "DbOpsConfigFailed");

  private final Type type;
  private final String reason;

  DbOpsEventReason(Type type, String reason) {
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
