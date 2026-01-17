/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterReplicateFromUsers;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterReplicateFrom {

  private ShardedClusterReplicateFromInstance instance;

  private ShardedClusterReplicateFromStorage storage;

  private ClusterReplicateFromUsers users;

  public ShardedClusterReplicateFromInstance getInstance() {
    return instance;
  }

  public void setInstance(ShardedClusterReplicateFromInstance instance) {
    this.instance = instance;
  }

  public ShardedClusterReplicateFromStorage getStorage() {
    return storage;
  }

  public void setStorage(ShardedClusterReplicateFromStorage storage) {
    this.storage = storage;
  }

  public ClusterReplicateFromUsers getUsers() {
    return users;
  }

  public void setUsers(ClusterReplicateFromUsers users) {
    this.users = users;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
