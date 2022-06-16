/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.common;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;

import io.stackgres.common.StackGresContainers;
import io.stackgres.operatorframework.resource.EventReason;

public enum ClusterBootstrapEventReason implements EventReason {

  CLUSTER_BOOTSTRAP_COMPLETED(NORMAL, "ClusterBootstrapCompleted");

  private final Type type;
  private final String reason;

  ClusterBootstrapEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
  }

  @Override
  public String component() {
    return StackGresContainers.DISTRIBUTEDLOGS_CONTROLLER.getName();
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
