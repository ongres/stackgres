/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Positive;
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

  @JsonProperty("index")
  @PositiveOrZero(message = "You need a shard index starting from zero")
  private int index;

  @JsonProperty("instancesPerCluster")
  @Positive(message = "You need at least 1 instance in each cluster")
  private Integer instancesPerCluster;

  @JsonProperty("replication")
  @Valid
  private StackGresShardedClusterReplication replicationForShards;

  @JsonProperty("configurations")
  @Valid
  private StackGresShardedClusterShardConfiguration configurationForShards;

  @JsonProperty("pods")
  @Valid
  private StackGresShardedClusterShardPod podForShards;

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

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
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

  public StackGresShardedClusterShardConfiguration getConfigurationForShards() {
    return configurationForShards;
  }

  public void setConfigurationForShards(
      StackGresShardedClusterShardConfiguration configurationForShards) {
    this.configurationForShards = configurationForShards;
  }

  public StackGresShardedClusterShardPod getPodForShards() {
    return podForShards;
  }

  public void setPodForShards(StackGresShardedClusterShardPod podForShards) {
    this.podForShards = podForShards;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(configurationForShards, index, instancesPerCluster,
        podForShards, replicationForShards);
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
    return Objects.equals(configurationForShards, other.configurationForShards)
        && index == other.index && Objects.equals(instancesPerCluster, other.instancesPerCluster)
        && Objects.equals(podForShards, other.podForShards)
        && Objects.equals(replicationForShards, other.replicationForShards);
  }

}
