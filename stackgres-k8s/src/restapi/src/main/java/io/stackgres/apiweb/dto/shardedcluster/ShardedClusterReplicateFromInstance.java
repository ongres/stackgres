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
public class ShardedClusterReplicateFromInstance {

  private String sgShardedCluster;

  private ShardedClusterReplicateFromExternal external;

  public String getSgShardedCluster() {
    return sgShardedCluster;
  }

  public void setSgShardedCluster(String sgShardedCluster) {
    this.sgShardedCluster = sgShardedCluster;
  }

  public ShardedClusterReplicateFromExternal getExternal() {
    return external;
  }

  public void setExternal(ShardedClusterReplicateFromExternal external) {
    this.external = external;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
