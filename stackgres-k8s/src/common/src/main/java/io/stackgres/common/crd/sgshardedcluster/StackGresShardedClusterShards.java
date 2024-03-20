/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true,
    value = { "instances", "postgres", "postgresServices",
        "initialData", "replicateFrom", "distributedLogs", "toInstallPostgresExtensions",
        "prometheusAutobind", "nonProductionOptions" })
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShards extends StackGresClusterSpec {

  @NotNull
  @PositiveOrZero(message = "clusters can not be negative")
  private Integer clusters;

  @NotNull
  @PositiveOrZero(message = "instances can not be negative")
  private Integer instancesPerCluster;

  @JsonProperty("replication")
  @Valid
  private StackGresShardedClusterReplication replicationForShards;

  @Valid
  private List<StackGresShardedClusterShard> overrides;

  @ReferencedField("replication.syncInstances")
  interface SyncInstances extends FieldReference { }

  @Override
  public boolean isPosgresSectionPresent() {
    return true;
  }

  @Override
  public boolean isPostgresServicesPresent() {
    return true;
  }

  @Override
  public boolean isInstancesPositive() {
    return true;
  }

  @Override
  public boolean isReplicationSectionPresent() {
    return true;
  }

  @Override
  public boolean isSupportingInstancesForInstancesInReplicationGroups() {
    return true;
  }

  @Override
  public boolean isSupportingMinInstancesForMinInstancesInReplicationGroups() {
    return true;
  }

  @Override
  public boolean isSupportingRequiredSynchronousReplicas() {
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "The total number synchronous replicas must be less or equals than the"
      + " number of coordinator or any shard replicas",
      payload = { SyncInstances.class })
  public boolean isShardsSupportingRequiredSynchronousReplicas() {
    return replicationForShards == null
        || !replicationForShards.isSynchronousMode()
        || replicationForShards.getSyncInstances() == null
        || getInstancesPerCluster() > replicationForShards.getSyncInstances();
  }

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

  public StackGresShardedClusterReplication getReplicationForShards() {
    return replicationForShards;
  }

  public void setReplicationForShards(StackGresShardedClusterReplication replicationForShards) {
    this.replicationForShards = replicationForShards;
  }

  public List<StackGresShardedClusterShard> getOverrides() {
    return overrides;
  }

  public void setOverrides(List<StackGresShardedClusterShard> overrides) {
    this.overrides = overrides;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(clusters, instancesPerCluster, overrides,
        replicationForShards);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresShardedClusterShards)) {
      return false;
    }
    StackGresShardedClusterShards other = (StackGresShardedClusterShards) obj;
    return Objects.equals(clusters, other.clusters)
        && Objects.equals(instancesPerCluster, other.instancesPerCluster)
        && Objects.equals(overrides, other.overrides)
        && Objects.equals(replicationForShards, other.replicationForShards);
  }

}
