/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresShardedClusterSpec {

  @JsonProperty("type")
  @ValidEnum(enumClass = StackGresShardingType.class, allowNulls = false,
      message = "only supported type is citus")
  private String type;

  @JsonProperty("database")
  @NotEmpty(message = "database name can not be empty")
  private String database;

  @JsonProperty("postgres")
  @NotNull(message = "postgres section is required")
  @Valid
  private StackGresClusterPostgres postgres;

  @JsonProperty("coordinator")
  @NotNull(message = "coordinator section is required")
  @Valid
  private StackGresShardedClusterCoordinator coordinator;

  @JsonProperty("shards")
  @NotNull(message = "shards section is required")
  @Valid
  private StackGresShardedClusterShards shards;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  @Valid
  private StackGresClusterNonProduction nonProductionOptions;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public StackGresClusterPostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(StackGresClusterPostgres postgres) {
    this.postgres = postgres;
  }

  public StackGresShardedClusterCoordinator getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(StackGresShardedClusterCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  public StackGresShardedClusterShards getShards() {
    return shards;
  }

  public void setShards(StackGresShardedClusterShards shards) {
    this.shards = shards;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  public StackGresClusterNonProduction getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(StackGresClusterNonProduction nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordinator, database, nonProductionOptions, postgres, prometheusAutobind,
        shards, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterSpec)) {
      return false;
    }
    StackGresShardedClusterSpec other = (StackGresShardedClusterSpec) obj;
    return Objects.equals(coordinator, other.coordinator)
        && Objects.equals(database, other.database)
        && Objects.equals(nonProductionOptions, other.nonProductionOptions)
        && Objects.equals(postgres, other.postgres)
        && Objects.equals(prometheusAutobind, other.prometheusAutobind)
        && Objects.equals(shards, other.shards)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
