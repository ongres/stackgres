/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;

public enum ClusterStatusCondition {

  PATRONI_REQUIRES_RESTART(Type.PENDING_RESTART, Status.TRUE, "PatroniRequiresRestart"),
  FALSE_PENDING_RESTART(Type.PENDING_RESTART, Status.FALSE, null),
  CLUSTER_CONFIG_ERROR(Type.FAILED, Status.TRUE, "ClusterConfigFailed"),
  FALSE_FAILED(Type.FAILED, Status.FALSE, null);

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
