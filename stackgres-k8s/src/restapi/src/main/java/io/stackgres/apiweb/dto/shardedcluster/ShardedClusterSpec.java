/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterNonProductionOptions;
import io.stackgres.apiweb.dto.cluster.ClusterPostgres;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterSpec {

  private String profile;

  private String type;

  private String database;

  private ClusterPostgres postgres;

  private ShardedClusterReplication replication;

  private ShardedClusterConfigurations configurations;

  private ShardedClusterSpecMetadata metadata;

  private ClusterDistributedLogs distributedLogs;

  private ShardedClusterPostgresServices postgresServices;

  private ShardedClusterCoordinator coordinator;

  private ShardedClusterShards shards;

  private ShardedClusterInitalData initialData;

  private ClusterNonProductionOptions nonProductionOptions;

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
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

  public ShardedClusterConfigurations getConfigurations() {
    return configurations;
  }

  public void setConfigurations(ShardedClusterConfigurations configurations) {
    this.configurations = configurations;
  }

  public ClusterNonProductionOptions getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(ClusterNonProductionOptions nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public ShardedClusterSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ShardedClusterSpecMetadata metadata) {
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

  public ShardedClusterInitalData getInitialData() {
    return initialData;
  }

  public void setInitialData(ShardedClusterInitalData initialData) {
    this.initialData = initialData;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
