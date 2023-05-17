/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterNonProduction;
import io.stackgres.apiweb.dto.cluster.ClusterPostgres;
import io.stackgres.apiweb.dto.cluster.ClusterSpecMetadata;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterSpec {

  @JsonProperty("type")
  private String type;

  @JsonProperty("database")
  private String database;

  @JsonProperty("postgres")
  private ClusterPostgres postgres;

  @JsonProperty("replication")
  private ShardedClusterReplication replication;

  @JsonProperty("configurations")
  private ShardedClusterConfiguration configuration;

  @JsonProperty("metadata")
  private ClusterSpecMetadata metadata;

  @JsonProperty("distributedLogs")
  private ClusterDistributedLogs distributedLogs;

  @JsonProperty("postgresServices")
  private ShardedClusterPostgresServices postgresServices;

  @JsonProperty("coordinator")
  private ShardedClusterCoordinator coordinator;

  @JsonProperty("shards")
  private ShardedClusterShards shards;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  private ClusterNonProduction nonProductionOptions;

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

  public ShardedClusterCoordinator getCoordinator() {
    return coordinator;
  }

  public void setCoordinator(ShardedClusterCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  public ShardedClusterShards getShards() {
    return shards;
  }

  public void setShards(ShardedClusterShards shards) {
    this.shards = shards;
  }

  public ClusterPostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(ClusterPostgres postgres) {
    this.postgres = postgres;
  }

  public ShardedClusterReplication getReplication() {
    return replication;
  }

  public void setReplication(ShardedClusterReplication replication) {
    this.replication = replication;
  }

  public ShardedClusterConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(ShardedClusterConfiguration configuration) {
    this.configuration = configuration;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  public ClusterNonProduction getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(ClusterNonProduction nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public ClusterSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ClusterSpecMetadata metadata) {
    this.metadata = metadata;
  }

  public ClusterDistributedLogs getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(ClusterDistributedLogs distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  public ShardedClusterPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(ShardedClusterPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
