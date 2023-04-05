/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterSpec {

  @JsonProperty("type")
  @ValidEnum(enumClass = StackGresShardingType.class, allowNulls = false,
      message = "only supported type is citus")
  private String type;

  @JsonProperty("database")
  @NotEmpty(message = "database name can not be empty")
  private String database;

  @JsonProperty("postgres")
  @NotNull(message = "postgres is required")
  @Valid
  private StackGresClusterPostgres postgres;

  @JsonProperty("postgresServices")
  @NotNull(message = "postgresServices is required")
  @Valid
  private StackGresShardedClusterPostgresServices postgresServices;

  @JsonProperty("replication")
  @Valid
  private StackGresShardedClusterReplication replication;

  @JsonProperty("configurations")
  @Valid
  private StackGresShardedClusterConfiguration configuration;

  @JsonProperty("metadata")
  @Valid
  private StackGresClusterSpecMetadata metadata;

  @JsonProperty("distributedLogs")
  @Valid
  private StackGresClusterDistributedLogs distributedLogs;

  @JsonProperty("coordinator")
  @NotNull(message = "coordinator is required")
  @Valid
  private StackGresShardedClusterCoordinator coordinator;

  @JsonProperty("shards")
  @NotNull(message = "shards is required")
  @Valid
  private StackGresShardedClusterShards shards;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  @Valid
  private StackGresClusterNonProduction nonProductionOptions;

  @ReferencedField("postgres")
  interface Postgres extends FieldReference { }

  @ReferencedField("replication")
  interface Replication extends FieldReference { }

  @ReferencedField("replication.syncInstances")
  interface SyncInstances extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "postgres is required", payload = { Postgres.class })
  public boolean isPosgresSectionPresent() {
    return postgres != null;
  }

  @JsonIgnore
  @AssertTrue(message = "replication is required", payload = { Replication.class })
  public boolean isReplicationSectionPresent() {
    return replication != null;
  }

  @JsonIgnore
  @AssertTrue(message = "The total number synchronous replicas must be less or equals than the"
      + " number of coordinator or any shard replicas",
      payload = { SyncInstances.class })
  public boolean isSupportingRequiredSynchronousReplicas() {
    return isCoordinatorSupportingRequiredSynchronousReplicas()
        && isShardsSupportingRequiredSynchronousReplicas()
        && isOverridesShardsSupportingRequiredSynchronousReplicas();
  }

  @JsonIgnore
  private boolean isCoordinatorSupportingRequiredSynchronousReplicas() {
    return coordinator == null
        || coordinator.getReplication() != null
        || replication == null
        || !replication.isSynchronousMode()
        || replication.getSyncInstances() == null
        || coordinator.getInstances() > replication.getSyncInstances();
  }

  @JsonIgnore
  private boolean isShardsSupportingRequiredSynchronousReplicas() {
    return shards == null
        || shards.getReplication() != null
        || replication == null
        || !replication.isSynchronousMode()
        || replication.getSyncInstances() == null
        || shards.getInstancesPerCluster() > replication.getSyncInstances();
  }

  @JsonIgnore
  private boolean isOverridesShardsSupportingRequiredSynchronousReplicas() {
    return shards == null
        || Optional.of(shards)
        .map(StackGresShardedClusterShards::getOverrides)
        .stream()
        .flatMap(List::stream)
        .allMatch(ovverideShard -> ovverideShard.getReplication() != null
        || shards.getReplication() != null
        || replication == null
        || !replication.isSynchronousMode()
        || replication.getSyncInstances() == null
        || ovverideShard.getInstancesPerCluster() == null
        || ovverideShard.getInstancesPerCluster() > replication.getSyncInstances());
  }

  public StackGresClusterSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(StackGresClusterSpecMetadata metadata) {
    this.metadata = metadata;
  }

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

  public StackGresShardedClusterReplication getReplication() {
    return replication;
  }

  public void setReplication(StackGresShardedClusterReplication replication) {
    this.replication = replication;
  }

  public StackGresShardedClusterConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(StackGresShardedClusterConfiguration configuration) {
    this.configuration = configuration;
  }

  public StackGresClusterDistributedLogs getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(StackGresClusterDistributedLogs distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  public StackGresShardedClusterPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(StackGresShardedClusterPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
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
    return Objects.hash(configuration, coordinator, database, distributedLogs, metadata,
        nonProductionOptions, postgres, postgresServices, prometheusAutobind, replication,
        type, shards);
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
    return Objects.equals(configuration, other.configuration)
        && Objects.equals(coordinator, other.coordinator)
        && Objects.equals(database, other.database)
        && Objects.equals(distributedLogs, other.distributedLogs)
        && Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProductionOptions, other.nonProductionOptions)
        && Objects.equals(postgres, other.postgres)
        && Objects.equals(postgresServices, other.postgresServices)
        && Objects.equals(prometheusAutobind, other.prometheusAutobind)
        && Objects.equals(replication, other.replication) && Objects.equals(type, other.type)
        && Objects.equals(shards, other.shards);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
