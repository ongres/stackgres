/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import static io.stackgres.operator.controller.EventReason.Type.NORMAL;
import static io.stackgres.operator.controller.EventReason.Type.WARNING;

public enum EventReason {

  CLUSTER_CREATED(NORMAL, "ClusterCreated"),
  CLUSTER_UPDATED(NORMAL, "ClusterUpdated"),
  CLUSTER_DELETED(NORMAL, "ClusterDeleted"),
  CLUSTER_CONFIG_ERROR(WARNING, "ClusterConfigFailed"),
  OPERATOR_ERROR(WARNING, "OperatorError");

  private final String type;
  private final String reason;

  EventReason(Type type, String reason) {
    this.type = type.type();
    this.reason = reason;
  }

  public String reason() {
    return reason;
  }

  public String type() {
    return type;
  }

  public enum Type {
    NORMAL("Normal"),
    WARNING("Warning");

    private final String type;

    Type(String type) {
      this.type = type;
    }

    public String type() {
      return type;
    }
  }
}
