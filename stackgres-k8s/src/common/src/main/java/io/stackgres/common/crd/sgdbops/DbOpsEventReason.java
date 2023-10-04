/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum DbOpsEventReason implements OperatorEventReason {

  DBOPS_CREATED(NORMAL, "DbOpsCreated"),
  DBOPS_UPDATED(NORMAL, "DbOpsUpdated"),
  DBOPS_DELETED(NORMAL, "DbOpsDeleted"),
  DBOPS_CONFIG_ERROR(WARNING, "DbOpsConfigFailed");

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
