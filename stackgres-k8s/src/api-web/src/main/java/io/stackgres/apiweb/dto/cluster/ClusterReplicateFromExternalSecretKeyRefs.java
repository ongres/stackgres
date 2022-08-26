/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterReplicateFromExternalSecretKeyRefs {

  @JsonProperty("superuser")
  private ClusterReplicateFromExternalSecretKeyRef superuser;

  @JsonProperty("replication")
  private ClusterReplicateFromExternalSecretKeyRef replication;

  @JsonProperty("authenticator")
  private ClusterReplicateFromExternalSecretKeyRef authenticator;

  public ClusterReplicateFromExternalSecretKeyRef getSuperuser() {
    return superuser;
  }

  public void setSuperuser(ClusterReplicateFromExternalSecretKeyRef superuser) {
    this.superuser = superuser;
  }

  public ClusterReplicateFromExternalSecretKeyRef getReplication() {
    return replication;
  }

  public void setReplication(ClusterReplicateFromExternalSecretKeyRef replication) {
    this.replication = replication;
  }

  public ClusterReplicateFromExternalSecretKeyRef getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(ClusterReplicateFromExternalSecretKeyRef authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authenticator, replication, superuser);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterReplicateFromExternalSecretKeyRefs)) {
      return false;
    }
    ClusterReplicateFromExternalSecretKeyRefs other =
        (ClusterReplicateFromExternalSecretKeyRefs) obj;
    return Objects.equals(authenticator, other.authenticator)
        && Objects.equals(replication, other.replication)
        && Objects.equals(superuser, other.superuser);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
