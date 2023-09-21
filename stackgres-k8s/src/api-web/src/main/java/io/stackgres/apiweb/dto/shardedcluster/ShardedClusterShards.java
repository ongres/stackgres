/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(value = { "instances", "postgres", "postgresServices",
    "initialData", "replicateFrom", "distributedLogs", "toInstallPostgresExtensions",
    "prometheusAutobind", "nonProductionOptions" })
public class ShardedClusterShards extends ClusterSpec {

  private Integer clusters;

  private Integer instancesPerCluster;

  @JsonProperty("replication")
  private ShardedClusterReplication replicationForShards;

  private List<ShardedClusterShard> overrides;

  @JsonProperty("configurations")
  private ShardedClusterInnerConfigurations configurationsForShards;

  public Integer getClusters() {
    return clusters;
  }

  public void setClusters(Integer clusters) {
    this.clusters = clusters;
  }

  public Integer getInstancesPerCluster() {
    return instancesPerCluster;
  }

  public void setInstancesPerCluster(Integer instancesPerCluster) {
    this.instancesPerCluster = instancesPerCluster;
  }

  public ShardedClusterReplication getReplicationForShards() {
    return replicationForShards;
  }

  public void setReplicationForShards(ShardedClusterReplication replicationForShards) {
    this.replicationForShards = replicationForShards;
  }

  public List<ShardedClusterShard> getOverrides() {
    return overrides;
  }

  public void setOverrides(List<ShardedClusterShard> overrides) {
    this.overrides = overrides;
  }

  public ShardedClusterInnerConfigurations getConfigurationsForShards() {
    return configurationsForShards;
  }

  public void setConfigurationsForShards(
      ShardedClusterInnerConfigurations configurationsForShards) {
    this.configurationsForShards = configurationsForShards;
  }

}
