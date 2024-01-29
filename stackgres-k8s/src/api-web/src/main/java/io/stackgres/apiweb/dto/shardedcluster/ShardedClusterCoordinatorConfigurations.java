/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ShardedClusterCoordinatorConfigurations extends ShardedClusterInnerConfigurations {

  private ShardedClusterShardingSphere shardingSphere;

  public ShardedClusterShardingSphere getShardingSphere() {
    return shardingSphere;
  }

  public void setShardingSphere(ShardedClusterShardingSphere shardingSphere) {
    this.shardingSphere = shardingSphere;
  }

}
