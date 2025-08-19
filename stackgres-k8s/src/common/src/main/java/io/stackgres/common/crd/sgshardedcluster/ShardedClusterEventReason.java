/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import static io.stackgres.operatorframework.resource.EventReason.Type.NORMAL;
import static io.stackgres.operatorframework.resource.EventReason.Type.WARNING;

import io.stackgres.common.crd.OperatorEventReason;

public enum ShardedClusterEventReason implements OperatorEventReason {

  SHARDED_CLUSTER_CREATED(NORMAL, "ShardedClusterCreated"),
  SHARDED_CLUSTER_UPDATED(NORMAL, "ShardedClusterUpdated"),
  SHARDED_CLUSTER_DELETED(NORMAL, "ShardedClusterDeleted"),
  SHARDED_CLUSTER_SECURITY_WARNING(WARNING, "ShardedClusterSecurityWarning"),
  SHARDED_CLUSTER_MAJOR_UPGRADE(WARNING, "ShardedClusterMajorUpgrade"),
  SHARDED_CLUSTER_CONFIG_ERROR(WARNING, "ShardedClusterConfigFailed");

  private final Type type;
  private final String reason;

  ShardedClusterEventReason(Type type, String reason) {
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
