/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterRestoreFromBackup {

  private String name;

  private Boolean targetInclusive;

  private ShardedClusterRestorePitr pointInTimeRecovery;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getTargetInclusive() {
    return targetInclusive;
  }

  public void setTargetInclusive(Boolean targetInclusive) {
    this.targetInclusive = targetInclusive;
  }

  public ShardedClusterRestorePitr getPointInTimeRecovery() {
    return pointInTimeRecovery;
  }

  public void setPointInTimeRecovery(ShardedClusterRestorePitr pointInTimeRecovery) {
    this.pointInTimeRecovery = pointInTimeRecovery;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
