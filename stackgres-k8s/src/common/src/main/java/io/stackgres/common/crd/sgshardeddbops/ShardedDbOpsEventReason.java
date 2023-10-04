/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum ShardedDbOpsEventReason implements OperatorEventReason {

  SHARDED_DBOPS_CREATED(NORMAL, "ShardedDbOpsCreated"),
  SHARDED_DBOPS_UPDATED(NORMAL, "ShardedDbOpsUpdated"),
  SHARDED_DBOPS_DELETED(NORMAL, "ShardedDbOpsDeleted"),
  SHARDED_DBOPS_CONFIG_ERROR(WARNING, "ShardedDbOpsConfigFailed");

  private final Type type;
  private final String reason;

  ShardedDbOpsEventReason(Type type, String reason) {
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
