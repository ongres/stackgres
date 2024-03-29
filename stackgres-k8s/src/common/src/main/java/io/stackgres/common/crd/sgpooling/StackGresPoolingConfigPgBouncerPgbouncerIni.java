/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresPoolingConfigPgBouncerPgbouncerIni {

  @NotNull(message = "pgbouncer should not be null")
  private Map<String, String> pgbouncer;

  @Valid
  private Map<String, Map<String, String>> databases;

  @Valid
  private Map<String, Map<String, String>> users;

  public Map<String, String> getPgbouncer() {
    return pgbouncer;
  }

  public void setPgbouncer(Map<String, String> pgbouncer) {
    this.pgbouncer = pgbouncer;
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
    return Objects.hash(databases, pgbouncer, users);
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
        && Objects.equals(pgbouncer, other.pgbouncer)
        && Objects.equals(users, other.users);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
