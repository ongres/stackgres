/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

public enum DistributedLogsStatusCondition {

  POD_REQUIRES_RESTART(Type.PENDING_RESTART, Status.TRUE, "PodRequiresRestart"),
  FALSE_PENDING_RESTART(Type.PENDING_RESTART, Status.FALSE, "FalsePendingRestart"),
  DISTRIBUTED_LOGS_CONFIG_ERROR(Type.FAILED, Status.TRUE, "DistributedLogsConfigFailed"),
  FALSE_FAILED(Type.FAILED, Status.FALSE, "FalseFailed");

  private final String type;
  private final String status;
  private final String reason;

  DistributedLogsStatusCondition(Type type, Status status, String reason) {
    this.type = type.getType();
    this.status = status.getStatus();
    this.reason = reason;
  }

  public StackGresDistributedLogsCondition getCondition() {
    return new StackGresDistributedLogsCondition(type, status, reason);
  }

  private enum Type {

    PENDING_RESTART("PendingRestart"),
    FAILED("Failed");

    private final String typeCondition;

    Type(String type) {
      this.typeCondition = type;
    }

    public String getType() {
      return typeCondition;
    }
  }

  private enum Status {

    TRUE("True"),
    FALSE("False"),
    UNKNOWN("Unknown");

    private final String statusCondition;

    Status(String statusCondition) {
      this.statusCondition = statusCondition;
    }

    public String getStatus() {
      return statusCondition;
    }
  }

}
