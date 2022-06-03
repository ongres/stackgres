/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operatorframework.resource.EventReason;

public enum ClusterControllerEventReason implements EventReason {

  CLUSTER_MANAGED_SQL(NORMAL, "ClusterManagedSql"),
  CLUSTER_MANAGED_SQL_ERROR(WARNING, "ClusterManagedSqlFailed"),
  CLUSTER_CONTROLLER_ERROR(WARNING, "ClusterControllerFailed");

  private final Type type;
  private final String reason;

  ClusterControllerEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return StackgresClusterContainers.CLUSTER_CONTROLLER;
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
