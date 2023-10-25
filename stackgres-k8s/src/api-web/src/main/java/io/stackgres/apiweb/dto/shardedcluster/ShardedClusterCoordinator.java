/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(value = { "postgres", "postgresServices",
    "initialData", "replicateFrom", "distributedLogs", "toInstallPostgresExtensions",
    "prometheusAutobind", "nonProductionOptions", "postgresServices" })
public class ShardedClusterCoordinator extends ClusterSpec {

  @JsonProperty("replication")
  private ShardedClusterReplication replicationForCoordinator;

  @JsonProperty("configurations")
  private ShardedClusterInnerConfigurations configurationsForCoordinator;

  public ShardedClusterReplication getReplicationForCoordinator() {
    return replicationForCoordinator;
  }

  public void setReplicationForCoordinator(
      ShardedClusterReplication replicationForCoordinator) {
    this.replicationForCoordinator = replicationForCoordinator;
  }

  public ShardedClusterInnerConfigurations getConfigurationsForCoordinator() {
    return configurationsForCoordinator;
  }

  public void setConfigurationsForCoordinator(
      ShardedClusterInnerConfigurations configurationsForCoordinator) {
    this.configurationsForCoordinator = configurationsForCoordinator;
  }

}
