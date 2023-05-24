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
@JsonIgnoreProperties(value = { "instances", "postgres", "postgresServices" })
public class ShardedClusterShards extends ClusterSpec {

  @JsonProperty("clusters")
  private int clusters;

  @JsonProperty("instancesPerCluster")
  private int instancesPerCluster;

  @JsonProperty("replication")
  private ShardedClusterReplication replicationForShards;

  @JsonProperty("overrides")
  private List<ShardedClusterShard> overrides;

  public int getClusters() {
    return clusters;
  }

  public void setClusters(int clusters) {
    this.clusters = clusters;
  }

  public int getInstancesPerCluster() {
    return instancesPerCluster;
  }

  public void setInstancesPerCluster(int instancesPerCluster) {
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

}
