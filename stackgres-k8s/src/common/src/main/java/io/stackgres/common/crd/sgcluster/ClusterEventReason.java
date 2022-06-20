/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum ClusterEventReason implements OperatorEventReason {

  CLUSTER_CREATED(NORMAL, "ClusterCreated"),
  CLUSTER_UPDATED(NORMAL, "ClusterUpdated"),
  CLUSTER_DELETED(NORMAL, "ClusterDeleted"),
  CLUSTER_SECURITY_WARNING(WARNING, "ClusterSecurityWarning"),
  CLUSTER_CONFIG_ERROR(WARNING, "ClusterConfigFailed");

  private final Type type;
  private final String reason;

  ClusterEventReason(Type type, String reason) {
    this.type = type;
    this.reason = reason;
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
