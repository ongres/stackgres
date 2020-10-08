/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum DistributedLogsEventReason implements OperatorEventReason {

  DISTRIBUTED_LOGS_CREATED(NORMAL, "DistributedLogsCreated"),
  DISTRIBUTED_LOGS_UPDATED(NORMAL, "DistributedLogsUpdated"),
  DISTRIBUTED_LOGS_DELETED(NORMAL, "DistributedLogsDeleted"),
  DISTRIBUTED_LOGS_CONFIG_ERROR(WARNING, "DistributedLogsConfigFailed");

  private final Type type;
  private final String reason;

  DistributedLogsEventReason(Type type, String reason) {
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
