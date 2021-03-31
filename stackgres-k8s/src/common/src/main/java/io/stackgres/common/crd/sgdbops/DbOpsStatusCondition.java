/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public enum DbOpsStatusCondition {

  DB_OPS_FALSE_RUNNING(Type.RUNNING, Status.FALSE, "OperationNotRunning"),
  DB_OPS_RUNNING(Type.RUNNING, Status.TRUE, "OperationRunning"),
  DB_OPS_FAILED(Type.FAILED, Status.TRUE, "OperationFailed"),
  DB_OPS_TIMED_OUT(Type.FAILED, Status.TRUE, "OperationTimedOut"),
  DB_OPS_LOCK_LOST(Type.FAILED, Status.TRUE, "OperationLockLost"),
  DB_OPS_FALSE_FAILED(Type.FAILED, Status.FALSE, "OperationNotFailed"),
  DB_OPS_COMPLETED(Type.COMPLETED, Status.TRUE, "OperationCompleted"),
  DB_OPS_FALSE_COMPLETED(Type.COMPLETED, Status.FALSE, "OperationNotCompleted");

  private final String type;
  private final String status;
  private final String reason;

  DbOpsStatusCondition(Type type, Status status, String reason) {
    this.type = type.getType();
    this.status = status.getStatus();
    this.reason = reason;
  }

  public StackGresDbOpsCondition getCondition() {
    return new StackGresDbOpsCondition(type, status, reason);
  }

  public StackGresDbOpsCondition buildCondition(){
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String currentDateTime = dateFormat.format(new Date());
    final StackGresDbOpsCondition stackGresDbOpsCondition =
        new StackGresDbOpsCondition(type, status, reason);
    stackGresDbOpsCondition.setLastTransitionTime(currentDateTime);
    return stackGresDbOpsCondition;
  }

  public boolean isCondition(StackGresDbOpsCondition condition) {
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

    public boolean isCondition(StackGresDbOpsCondition condition) {
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

    public boolean isCondition(StackGresDbOpsCondition condition) {
      return Objects.equals(condition.getStatus(), status);
    }
  }

}
