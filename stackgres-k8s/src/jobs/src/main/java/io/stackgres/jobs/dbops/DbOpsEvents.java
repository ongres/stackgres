/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import io.stackgres.jobs.configuration.JobsProperty;
import io.stackgres.operatorframework.resource.EventReason;

public enum DbOpsEvents implements EventReason {
  DB_OP_STARTED(Type.NORMAL, "DbOpStarted"),
  DB_OP_TIMEOUT(Type.WARNING, "DbOpTimeOut"),
  DB_OP_FAILED(Type.WARNING, "DbOpFailed"),
  DB_OP_COMPLETED(Type.NORMAL, "DbOpCompleted");

  private final Type type;
  private final String reason;

  DbOpsEvents(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return JobsProperty.DATABASE_OPERATION_CR_NAME.getString();
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
