/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterCoordinatorConfigurations extends ShardedClusterInnerConfigurations {

  private ShardedClusterShardingSphere shardingSphere;

  public ShardedClusterShardingSphere getShardingSphere() {
    return shardingSphere;
  }

  public void setShardingSphere(ShardedClusterShardingSphere shardingSphere) {
    this.shardingSphere = shardingSphere;
  }

}
