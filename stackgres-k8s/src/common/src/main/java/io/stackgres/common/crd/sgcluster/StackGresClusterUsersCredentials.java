/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterUsersCredentials {

  @JsonProperty("superuser")
  @Valid
  private StackGresClusterUserSecretKeyRef superuser;

  @JsonProperty("replication")
  @Valid
  private StackGresClusterUserSecretKeyRef replication;

  @JsonProperty("authenticator")
  @Valid
  private StackGresClusterUserSecretKeyRef authenticator;

  public StackGresClusterUserSecretKeyRef getSuperuser() {
    return superuser;
  }

  public void setSuperuser(StackGresClusterUserSecretKeyRef superuser) {
    this.superuser = superuser;
  }

  public StackGresClusterUserSecretKeyRef getReplication() {
    return replication;
  }

  public void setReplication(StackGresClusterUserSecretKeyRef replication) {
    this.replication = replication;
  }

  public StackGresClusterUserSecretKeyRef getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(StackGresClusterUserSecretKeyRef authenticator) {
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
    if (!(obj instanceof StackGresClusterUsersCredentials)) {
      return false;
    }
    StackGresClusterUsersCredentials other =
        (StackGresClusterUsersCredentials) obj;
    return Objects.equals(authenticator, other.authenticator)
        && Objects.equals(replication, other.replication)
        && Objects.equals(superuser, other.superuser);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
