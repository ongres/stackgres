/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterReplicateFrom {

  @JsonProperty("instance")
  private ClusterReplicateFromInstance instance;

  @JsonProperty("storage")
  private ClusterReplicateFromStorage storage;

  @JsonProperty("users")
  private ClusterReplicateFromUsers users;

  public ClusterReplicateFromInstance getInstance() {
    return instance;
  }

  public void setInstance(ClusterReplicateFromInstance instance) {
    this.instance = instance;
  }

  public ClusterReplicateFromStorage getStorage() {
    return storage;
  }

  public void setStorage(ClusterReplicateFromStorage storage) {
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
