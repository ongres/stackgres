/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import io.stackgres.common.crd.Condition;

public enum ShardedClusterStatusCondition {

  SHARDED_CLUSTER_BOOTSTRAPPED(Type.BOOTSTRAPPED, Status.TRUE, "ShardedClusterBootstrapped"),
  SHARDED_CLUSTER_REQUIRES_RESTART(Type.PENDING_RESTART, Status.TRUE, "ShardedClusterRequiresRestart"),
  FALSE_PENDING_RESTART(Type.PENDING_RESTART, Status.FALSE, "FalsePendingRestart"),
  SHARDED_CLUSTER_REQUIRES_UPGRADE(
      Type.PENDING_UPGRADE, Status.TRUE, "ShardedClusterRequiresUpgrade"),
  FALSE_PENDING_UPGRADE(Type.PENDING_UPGRADE, Status.FALSE, "FalsePendingUpgrade"),
  SHARDED_CLUSTER_CONFIG_ERROR(Type.FAILED, Status.TRUE, "ShardedClusterConfigFailed"),
  FALSE_FAILED(Type.FAILED, Status.FALSE, "FalseFailed");

  private final String type;
  private final String status;
  private final String reason;

  ShardedClusterStatusCondition(Type type, Status status, String reason) {
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

    BOOTSTRAPPED("Bootstrapped"),
    PENDING_RESTART("PendingRestart"),
    PENDING_UPGRADE("PendingUpgrade"),
    FAILED("Failed");

    private final String typeCondition;

    Type(String type) {
      this.typeCondition = type;
    }

    public String getType() {
      return typeCondition;
    }
  }

  public enum Status {

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
