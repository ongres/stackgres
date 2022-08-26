/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterReplicateFromExternalSecretKeyRefs {

  @JsonProperty("superuser")
  @NotNull(message = "superuser section is required")
  @Valid
  private StackGresClusterReplicateFromExternalSecretKeyRef superuser;

  @JsonProperty("replication")
  @NotNull(message = "replication section is required")
  @Valid
  private StackGresClusterReplicateFromExternalSecretKeyRef replication;

  @JsonProperty("authenticator")
  @NotNull(message = "authenticator section is required")
  @Valid
  private StackGresClusterReplicateFromExternalSecretKeyRef authenticator;

  public StackGresClusterReplicateFromExternalSecretKeyRef getSuperuser() {
    return superuser;
  }

  public void setSuperuser(StackGresClusterReplicateFromExternalSecretKeyRef superuser) {
    this.superuser = superuser;
  }

  public StackGresClusterReplicateFromExternalSecretKeyRef getReplication() {
    return replication;
  }

  public void setReplication(StackGresClusterReplicateFromExternalSecretKeyRef replication) {
    this.replication = replication;
  }

  public StackGresClusterReplicateFromExternalSecretKeyRef getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(StackGresClusterReplicateFromExternalSecretKeyRef authenticator) {
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
    if (!(obj instanceof StackGresClusterReplicateFromExternalSecretKeyRefs)) {
      return false;
    }
    StackGresClusterReplicateFromExternalSecretKeyRefs other =
        (StackGresClusterReplicateFromExternalSecretKeyRefs) obj;
    return Objects.equals(authenticator, other.authenticator)
        && Objects.equals(replication, other.replication)
        && Objects.equals(superuser, other.superuser);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
