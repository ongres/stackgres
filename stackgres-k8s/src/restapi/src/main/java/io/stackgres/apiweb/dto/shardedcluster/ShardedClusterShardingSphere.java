/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.config.ShardingSphereServiceAccount;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShardedClusterShardingSphere {

  private ShardedClusterShardingSphereMode mode;

  private ShardedClusterShardingSphereAuthority authority;

  private ShardingSphereServiceAccount serviceAccount;

  public ShardedClusterShardingSphereMode getMode() {
    return mode;
  }

  public void setMode(ShardedClusterShardingSphereMode mode) {
    this.mode = mode;
  }

  public ShardedClusterShardingSphereAuthority getAuthority() {
    return authority;
  }

  public void setAuthority(ShardedClusterShardingSphereAuthority authority) {
    this.authority = authority;
  }

  public ShardingSphereServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ShardingSphereServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
