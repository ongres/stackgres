/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterCredentials {

  @JsonProperty("patroni")
  @Valid
  private StackGresClusterPatroniCredentials patroni;

  @JsonProperty("users")
  @Valid
  private StackGresClusterUsersCredentials users;

  public StackGresClusterPatroniCredentials getPatroni() {
    return patroni;
  }

  public void setPatroni(StackGresClusterPatroniCredentials patroni) {
    this.patroni = patroni;
  }

  public StackGresClusterUsersCredentials getUsers() {
    return users;
  }

  public void setUsers(StackGresClusterUsersCredentials users) {
    this.users = users;
  }

  @Override
  public int hashCode() {
    return Objects.hash(patroni, users);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterCredentials)) {
      return false;
    }
    StackGresClusterCredentials other = (StackGresClusterCredentials) obj;
    return Objects.equals(patroni, other.patroni) && Objects.equals(users, other.users);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
