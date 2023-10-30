/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true,
    value = { "instances", "postgres", "postgresServices",
        "initialData", "replicateFrom", "distributedLogs", "toInstallPostgresExtensions",
        "prometheusAutobind", "nonProductionOptions" })
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShard extends StackGresClusterSpec {

  @PositiveOrZero(message = "You need a shard index starting from zero")
  private Integer index;

  @PositiveOrZero(message = "instances can not be negative")
  private Integer instancesPerCluster;

  @JsonProperty("replication")
  @Valid
  private StackGresShardedClusterReplication replicationForShards;

  @JsonProperty("configurations")
  @Valid
  private StackGresShardedClusterShardConfigurations configurationsForShards;

  @JsonProperty("pods")
  @Valid
  private StackGresShardedClusterShardPods podsForShards;

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
  public boolean isResourceProfilePresent() {
    return true;
  }

  @Override
  public boolean isConfigurationsSectionPresent() {
    return true;
  }

  @Override
  public boolean isPodsSectionPresent() {
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
  public boolean isSupportingRequiredSynchronousReplicas() {
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "The total number synchronous replicas must be less or equals than the"
      + " number of coordinator or any shard replicas",
      payload = { SyncInstances.class })
  public boolean isShardsOverrideSupportingRequiredSynchronousReplicas() {
    return replicationForShards == null
        || !replicationForShards.isSynchronousMode()
        || replicationForShards.getSyncInstances() == null
        || getInstancesPerCluster() > replicationForShards.getSyncInstances();
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
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

  public StackGresShardedClusterShardConfigurations getConfigurationsForShards() {
    return configurationsForShards;
  }

  public void setConfigurationsForShards(
      StackGresShardedClusterShardConfigurations configurationsForShards) {
    this.configurationsForShards = configurationsForShards;
  }

  public StackGresShardedClusterShardPods getPodsForShards() {
    return podsForShards;
  }

  public void setPodsForShards(StackGresShardedClusterShardPods podsForShards) {
    this.podsForShards = podsForShards;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + Objects.hash(configurationsForShards, index, instancesPerCluster, podsForShards,
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
    if (!(obj instanceof StackGresShardedClusterShard)) {
      return false;
    }
    StackGresShardedClusterShard other = (StackGresShardedClusterShard) obj;
    return Objects.equals(configurationsForShards, other.configurationsForShards)
        && Objects.equals(index, other.index)
        && Objects.equals(instancesPerCluster, other.instancesPerCluster)
        && Objects.equals(podsForShards, other.podsForShards)
        && Objects.equals(replicationForShards, other.replicationForShards);
  }

}
