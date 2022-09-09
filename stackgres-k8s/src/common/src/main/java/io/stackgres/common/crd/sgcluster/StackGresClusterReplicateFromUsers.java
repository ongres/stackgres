/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFromUsers {

  @JsonProperty("superuser")
  @NotNull(message = "superuser section is required")
  @Valid
  private StackGresClusterReplicateFromUserSecretKeyRef superuser;

  @JsonProperty("replication")
  @NotNull(message = "replication section is required")
  @Valid
  private StackGresClusterReplicateFromUserSecretKeyRef replication;

  @JsonProperty("authenticator")
  @NotNull(message = "authenticator section is required")
  @Valid
  private StackGresClusterReplicateFromUserSecretKeyRef authenticator;

  public StackGresClusterReplicateFromUserSecretKeyRef getSuperuser() {
    return superuser;
  }

  public void setSuperuser(StackGresClusterReplicateFromUserSecretKeyRef superuser) {
    this.superuser = superuser;
  }

  public StackGresClusterReplicateFromUserSecretKeyRef getReplication() {
    return replication;
  }

  public void setReplication(StackGresClusterReplicateFromUserSecretKeyRef replication) {
    this.replication = replication;
  }

  public StackGresClusterReplicateFromUserSecretKeyRef getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(StackGresClusterReplicateFromUserSecretKeyRef authenticator) {
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
    if (!(obj instanceof StackGresClusterReplicateFromUsers)) {
      return false;
    }
    StackGresClusterReplicateFromUsers other =
        (StackGresClusterReplicateFromUsers) obj;
    return Objects.equals(authenticator, other.authenticator)
        && Objects.equals(replication, other.replication)
        && Objects.equals(superuser, other.superuser);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
