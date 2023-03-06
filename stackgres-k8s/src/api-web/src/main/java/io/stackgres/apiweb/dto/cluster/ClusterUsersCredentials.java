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
public class ClusterUsersCredentials {

  @JsonProperty("superuser")
  private ClusterReplicateFromUserSecretKeyRef superuser;

  @JsonProperty("replication")
  private ClusterReplicateFromUserSecretKeyRef replication;

  @JsonProperty("authenticator")
  private ClusterReplicateFromUserSecretKeyRef authenticator;

  public ClusterReplicateFromUserSecretKeyRef getSuperuser() {
    return superuser;
  }

  public void setSuperuser(ClusterReplicateFromUserSecretKeyRef superuser) {
    this.superuser = superuser;
  }

  public ClusterReplicateFromUserSecretKeyRef getReplication() {
    return replication;
  }

  public void setReplication(ClusterReplicateFromUserSecretKeyRef replication) {
    this.replication = replication;
  }

  public ClusterReplicateFromUserSecretKeyRef getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(ClusterReplicateFromUserSecretKeyRef authenticator) {
    this.authenticator = authenticator;
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
