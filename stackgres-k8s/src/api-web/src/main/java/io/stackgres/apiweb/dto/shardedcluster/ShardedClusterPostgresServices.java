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
public class ShardedClusterPostgresServices {

  private ShardedClusterPostgresCoordinatorServices coordinator;

  private ShardedClusterPostgresShardsServices shards;

  public ShardedClusterPostgresCoordinatorServices getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(ShardedClusterPostgresCoordinatorServices coordinator) {
    this.coordinator = coordinator;
  }

  public ShardedClusterPostgresShardsServices getShards() {
    return shards;
  }

  public void setShards(ShardedClusterPostgresShardsServices shards) {
    this.shards = shards;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
