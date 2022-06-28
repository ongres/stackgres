/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.common;

import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.StackGresContainer;
import io.stackgres.operatorframework.resource.EventReason;

public enum DistributedLogsControllerEventReason implements EventReason {

  DISTRIBUTEDLOGS_CONTROLLER_ERROR(WARNING, "DistributedLogsControllerFailed");

  private final Type type;
  private final String reason;

  DistributedLogsControllerEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return StackGresContainer.DISTRIBUTEDLOGS_CONTROLLER.getName();
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
