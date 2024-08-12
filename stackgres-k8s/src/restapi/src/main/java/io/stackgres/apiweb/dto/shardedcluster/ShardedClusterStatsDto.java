/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterStatsDto extends ResourceDto {

  private ShardedClusterClusterStats coordinator;

  private ShardedClusterClusterStats shards;

  public ShardedClusterClusterStats getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(ShardedClusterClusterStats coordinator) {
    this.coordinator = coordinator;
  }

  public ShardedClusterClusterStats getShards() {
    return shards;
  }

  public void setShards(ShardedClusterClusterStats shards) {
    this.shards = shards;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
