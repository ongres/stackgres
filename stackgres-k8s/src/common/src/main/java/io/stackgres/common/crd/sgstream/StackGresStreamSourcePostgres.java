/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamSourcePostgres {

  @NotNull
  private String host;

  private Integer port;

  private String database;

  @Valid
  private SecretKeySelector username;

  @Valid
  private SecretKeySelector password;

  private List<String> includes;

  private List<String> excludes;

  @Valid
  private StackGresStreamSourcePostgresDebeziumProperties debeziumProperties;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public SecretKeySelector getUsername() {
    return username;
  }

  public void setUsername(SecretKeySelector username) {
    this.username = username;
  }

  public SecretKeySelector getPassword() {
    return password;
  }

  public void setPassword(SecretKeySelector password) {
    this.password = password;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public StackGresStreamSourcePostgresDebeziumProperties getDebeziumProperties() {
    return debeziumProperties;
  }

  public void setDebeziumProperties(
      StackGresStreamSourcePostgresDebeziumProperties debeziumProperties) {
    this.debeziumProperties = debeziumProperties;
  }

  @Override
  public int hashCode() {
    return Objects.hash(database, debeziumProperties, excludes, host, includes, password,
        port, username);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamSourcePostgres)) {
      return false;
    }
    StackGresStreamSourcePostgres other = (StackGresStreamSourcePostgres) obj;
    return Objects.equals(database, other.database)
        && Objects.equals(debeziumProperties, other.debeziumProperties)
        && Objects.equals(excludes, other.excludes) && Objects.equals(host, other.host)
        && Objects.equals(includes, other.includes) && Objects.equals(password, other.password)
        && Objects.equals(port, other.port) && Objects.equals(username, other.username);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
