/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.stackgres.jobs.app.JobsProperty;
import io.stackgres.operatorframework.resource.EventReason;

public enum RestartEventType implements EventReason {

  INCREASING_INSTANCES(Type.NORMAL, "IncreasingInstances"),
  INSTANCES_INCREASED(Type.NORMAL, "InstancesIncreased"),
  RESTARTING_POD(Type.NORMAL, "RestartingPod"),
  POD_RESTARTED(Type.NORMAL, "PodRestarted"),
  DECREASING_INSTANCES(Type.NORMAL, "DecreasingInstances"),
  INSTANCES_DECREASED(Type.NORMAL, "InstancesDecreased"),
  SWITCHOVER_INITIATED(Type.NORMAL, "SwitchoverInitiated"),
  SWITCHOVER_FINALIZED(Type.NORMAL, "SwitchoverFinalized"),
  RESTARTING_POSTGRES(Type.NORMAL, "RestartingPostgres"),
  POSTGRES_RESTARTED(Type.NORMAL, "PostgresRestarted"),
  POSTGRES_RESTART_FAILED(Type.WARNING, "PostgresRestartFailed");

  private final Type type;
  private final String reason;

  RestartEventType(Type type, String reason) {
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
