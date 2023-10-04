/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import java.util.Objects;

import io.stackgres.common.crd.Condition;

public enum ShardedDbOpsStatusCondition {

  DBOPS_FALSE_RUNNING(Type.RUNNING, Status.FALSE, "OperationNotRunning"),
  DBOPS_RUNNING(Type.RUNNING, Status.TRUE, "OperationRunning"),
  DBOPS_FAILED(Type.FAILED, Status.TRUE, "OperationFailed"),
  DBOPS_TIMED_OUT(Type.FAILED, Status.TRUE, "OperationTimedOut"),
  DBOPS_LOCK_LOST(Type.FAILED, Status.TRUE, "OperationLockLost"),
  DBOPS_FALSE_FAILED(Type.FAILED, Status.FALSE, "OperationNotFailed"),
  DBOPS_COMPLETED(Type.COMPLETED, Status.TRUE, "OperationCompleted"),
  DBOPS_FALSE_COMPLETED(Type.COMPLETED, Status.FALSE, "OperationNotCompleted");

  private final String type;
  private final String status;
  private final String reason;

  ShardedDbOpsStatusCondition(Type type, Status status, String reason) {
    this.type = type.getType();
    this.status = status.getStatus();
    this.reason = reason;
  }

  public Condition getCondition() {
    return new Condition(type, status, reason);
  }

  public boolean isCondition(Condition condition) {
    return Objects.equals(condition.getType(), type)
        && Objects.equals(condition.getStatus(), status)
        && Objects.equals(condition.getReason(), reason);
  }

  public enum Type {
    RUNNING("Running"),
    FAILED("Failed"),
    COMPLETED("Completed");

    private final String type;

    Type(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }

    public boolean isCondition(Condition condition) {
      return Objects.equals(condition.getType(), type);
    }
  }

  public enum Status {
    TRUE("True"),
    FALSE("False"),
    UNKNOWN("Unknown");

    private final String status;

    Status(String status) {
      this.status = status;
    }

    public String getStatus() {
      return status;
    }

    public boolean isCondition(Condition condition) {
      return Objects.equals(condition.getStatus(), status);
    }
  }

}
