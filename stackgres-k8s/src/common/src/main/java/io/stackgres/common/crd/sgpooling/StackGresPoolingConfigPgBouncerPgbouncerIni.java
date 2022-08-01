/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

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
public class StackGresPoolingConfigPgBouncerPgbouncerIni {

  @JsonProperty("pgbouncer")
  @NotEmpty(message = "pgbouncer should not be empty")
  private Map<String, String> parameters;

  @JsonProperty("databases")
  @Valid
  private Map<String, Map<String, String>> databases;

  @JsonProperty("users")
  @Valid
  private Map<String, Map<String, String>> users;

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public Map<String, Map<String, String>> getDatabases() {
    return databases;
  }

  public void setDatabases(Map<String, Map<String, String>> databases) {
    this.databases = databases;
  }

  public Map<String, Map<String, String>> getUsers() {
    return users;
  }

  public void setUsers(Map<String, Map<String, String>> users) {
    this.users = users;
  }

  @Override
  public int hashCode() {
    return Objects.hash(databases, parameters, users);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfigPgBouncerPgbouncerIni)) {
      return false;
    }
    StackGresPoolingConfigPgBouncerPgbouncerIni other =
        (StackGresPoolingConfigPgBouncerPgbouncerIni) obj;
    return Objects.equals(databases, other.databases)
        && Objects.equals(parameters, other.parameters)
        && Objects.equals(users, other.users);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
