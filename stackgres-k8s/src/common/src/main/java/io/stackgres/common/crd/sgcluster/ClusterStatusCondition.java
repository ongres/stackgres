/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

public enum ClusterStatusCondition {

  POD_REQUIRES_RESTART(Type.PENDING_RESTART, Status.TRUE, "PodRequiresRestart"),
  FALSE_PENDING_RESTART(Type.PENDING_RESTART, Status.FALSE, "FalsePendingRestart"),
  CLUSTER_REQUIRES_UPGRADE(Type.PENDING_UPGRADE, Status.TRUE, "ClusterRequiresUpgrade"),
  FALSE_PENDING_UPGRADE(Type.PENDING_UPGRADE, Status.FALSE, "FalsePendingUpgrade"),
  CLUSTER_CONFIG_ERROR(Type.FAILED, Status.TRUE, "ClusterConfigFailed"),
  FALSE_FAILED(Type.FAILED, Status.FALSE, "FalseFailed");

  private final String type;
  private final String status;
  private final String reason;

  ClusterStatusCondition(Type type, Status status, String reason) {
    this.type = type.getType();
    this.status = status.getStatus();
    this.reason = reason;
  }

  public StackGresClusterCondition getCondition() {
    return new StackGresClusterCondition(type, status, reason);
  }

  public enum Type {

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
