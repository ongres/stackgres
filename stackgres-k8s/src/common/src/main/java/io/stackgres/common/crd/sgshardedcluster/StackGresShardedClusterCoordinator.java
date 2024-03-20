/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true,
    value = { "postgres", "postgresServices",
        "initialData", "replicateFrom", "distributedLogs", "toInstallPostgresExtensions",
        "prometheusAutobind", "nonProductionOptions" })
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterCoordinator extends StackGresClusterSpec {

  @JsonProperty("configurations")
  @Valid
  private StackGresShardedClusterCoordinatorConfigurations configurationsForCoordinator;

  @JsonProperty("replication")
  @Valid
  private StackGresShardedClusterReplication replicationForCoordinator;

  @ReferencedField("replication.syncInstances")
  interface SyncInstances extends FieldReference { }

  @ReferencedField("configurations")
  interface Configurations extends FieldReference { }

  @Override
  public boolean isPosgresSectionPresent() {
    return true;
  }

  @Override
  public boolean isPostgresServicesPresent() {
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

  @Override
  public boolean isConfigurationsSectionPresent() {
    return true;
  }

  @JsonIgnore
  @AssertTrue(message = "configurations is required", payload = { Configurations.class })
  public boolean isCoordinatorConfigurationsSectionPresent() {
    return configurationsForCoordinator != null;
  }

  @JsonIgnore
  @AssertTrue(message = "The total number synchronous replicas must be less or equals than the"
      + " number of coordinator or any shard replicas",
      payload = { SyncInstances.class })
  public boolean isCoordinatorSupportingRequiredSynchronousReplicas() {
    return replicationForCoordinator == null
        || !replicationForCoordinator.isSynchronousMode()
        || replicationForCoordinator.getSyncInstances() == null
        || getInstances() > replicationForCoordinator.getSyncInstances();
  }

  public StackGresShardedClusterCoordinatorConfigurations getConfigurationsForCoordinator() {
    return configurationsForCoordinator;
  }

  public void setConfigurationsForCoordinator(
      StackGresShardedClusterCoordinatorConfigurations configurationsForCoordinator) {
    this.configurationsForCoordinator = configurationsForCoordinator;
  }

  public StackGresShardedClusterReplication getReplicationForCoordinator() {
    return replicationForCoordinator;
  }

  public void setReplicationForCoordinator(
      StackGresShardedClusterReplication replicationForCoordinator) {
    this.replicationForCoordinator = replicationForCoordinator;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(configurationsForCoordinator, replicationForCoordinator);
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
    if (!(obj instanceof StackGresShardedClusterCoordinator)) {
      return false;
    }
    StackGresShardedClusterCoordinator other = (StackGresShardedClusterCoordinator) obj;
    return Objects.equals(configurationsForCoordinator, other.configurationsForCoordinator)
        && Objects.equals(replicationForCoordinator, other.replicationForCoordinator);
  }

}
