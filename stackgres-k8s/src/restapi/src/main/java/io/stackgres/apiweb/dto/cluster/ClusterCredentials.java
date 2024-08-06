/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterCredentials {

  private ClusterPatroniCredentials patroni;

  private ClusterUsersCredentials users;

  public ClusterPatroniCredentials getPatroni() {
    return patroni;
  }

  public void setPatroni(ClusterPatroniCredentials patroni) {
    this.patroni = patroni;
  }

  public ClusterUsersCredentials getUsers() {
    return users;
  }

  public void setUsers(ClusterUsersCredentials users) {
    this.users = users;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
