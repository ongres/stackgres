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
public class StackGresStreamSourceSgCluster {

  @NotNull
  private String name;

  private String database;

  @Valid
  private SecretKeySelector username;

  @Valid
  private SecretKeySelector password;

  private List<String> includes;

  private List<String> excludes;

  private Boolean skipDropReplicationSlotAndPublicationOnTombstone;

  @Valid
  private StackGresStreamSourcePostgresDebeziumProperties debeziumProperties;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public Boolean getSkipDropReplicationSlotAndPublicationOnTombstone() {
    return skipDropReplicationSlotAndPublicationOnTombstone;
  }

  public void setSkipDropReplicationSlotAndPublicationOnTombstone(
      Boolean skipDropReplicationSlotAndPublicationOnTombstone) {
    this.skipDropReplicationSlotAndPublicationOnTombstone = skipDropReplicationSlotAndPublicationOnTombstone;
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
    return Objects.hash(database, debeziumProperties, excludes, includes, name, password,
        skipDropReplicationSlotAndPublicationOnTombstone, username);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamSourceSgCluster)) {
      return false;
    }
    StackGresStreamSourceSgCluster other = (StackGresStreamSourceSgCluster) obj;
    return Objects.equals(database, other.database)
        && Objects.equals(debeziumProperties, other.debeziumProperties)
        && Objects.equals(excludes, other.excludes) && Objects.equals(includes, other.includes)
        && Objects.equals(name, other.name) && Objects.equals(password, other.password)
        && Objects.equals(skipDropReplicationSlotAndPublicationOnTombstone,
            other.skipDropReplicationSlotAndPublicationOnTombstone)
        && Objects.equals(username, other.username);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
