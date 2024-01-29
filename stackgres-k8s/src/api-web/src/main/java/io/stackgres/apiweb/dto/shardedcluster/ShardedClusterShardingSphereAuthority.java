/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShardedClusterShardingSphereAuthority {

  private List<ShardedClusterShardingSphereUser> users;

  private ShardedClusterShardingSpherePrivilege privilege;

  public List<ShardedClusterShardingSphereUser> getUsers() {
    return users;
  }

  public void setUsers(List<ShardedClusterShardingSphereUser> users) {
    this.users = users;
  }

  public ShardedClusterShardingSpherePrivilege getPrivilege() {
    return privilege;
  }

  public void setPrivilege(ShardedClusterShardingSpherePrivilege privilege) {
    this.privilege = privilege;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
