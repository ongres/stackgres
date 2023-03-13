/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterSpec {

  @JsonProperty("postgres")
  @Valid
  private StackGresClusterPostgres postgres;

  @JsonProperty("instances")
  private int instances;

  @JsonProperty("replication")
  @Valid
  private StackGresClusterReplication replication;

  @JsonProperty("configurations")
  @NotNull(message = "configurations section is required")
  @Valid
  private StackGresClusterConfiguration configuration;

  @JsonProperty("sgInstanceProfile")
  @NotNull(message = "resource profile is required")
  private String resourceProfile;

  @JsonProperty("initialData")
  @Valid
  private StackGresClusterInitData initData;

  @JsonProperty("replicateFrom")
  @Valid
  private StackGresClusterReplicateFrom replicateFrom;

  @JsonProperty("managedSql")
  @Valid
  private StackGresClusterManagedSql managedSql;

  @JsonProperty("pods")
  @NotNull(message = "pods section is required")
  @Valid
  private StackGresClusterPod pod;

  @JsonProperty("distributedLogs")
  @Valid
  private StackGresClusterDistributedLogs distributedLogs;

  @JsonProperty("toInstallPostgresExtensions")
  @Valid
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  @Valid
  private StackGresClusterNonProduction nonProductionOptions;

  @JsonProperty("postgresServices")
  @Valid
  private StackGresClusterPostgresServices postgresServices;

  @JsonProperty("metadata")
  @Valid
  private StackGresClusterSpecMetadata metadata;

  @ReferencedField("postgres")
  interface Postgres extends FieldReference { }

  @ReferencedField("replication")
  interface Replication extends FieldReference { }

  @ReferencedField("instances")
  interface Instances extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "postgres section is required", payload = { Postgres.class })
  public boolean isPosgresSectionPresent() {
    return postgres != null;
  }

  @JsonIgnore
  @AssertTrue(message = "You need at least 1 instance in the cluster",
      payload = { Instances.class })
  public boolean isInstancesPositive() {
    return instances > 0;
  }

  @JsonIgnore
  @AssertTrue(message = "replication section is required", payload = { Replication.class })
  public boolean isReplicationSectionPresent() {
    return replication != null;
  }

  @JsonIgnore
  @AssertTrue(message = "The total number of instances must be greather than the number of"
      + " instances in replication groups", payload = { Instances.class })
  public boolean isSupportingInstancesForInstancesInReplicationGroups() {
    return instances > getInstancesInReplicationGroups();
  }

  @JsonIgnore
  @AssertTrue(message = "The total number of instances must be greather than the number of"
      + " synchronous replicas", payload = { Instances.class })
  public boolean isSupportingRequiredSynchronousReplicas() {
    return replication == null
        || !replication.isSynchronousMode()
        || replication.getSyncInstances() == null
        || instances > replication.getSyncInstances();
  }

  @JsonIgnore
  public int getInstancesInImplicitReplicationGroup() {
    return instances - getInstancesInReplicationGroups();
  }

  @JsonIgnore
  public int getInstancesInReplicationGroups() {
    if (replication == null
        || replication.getGroups() == null) {
      return 0;
    }
    return replication.getGroups().stream()
        .map(StackGresClusterReplicationGroup::getInstances)
        .reduce(0, (sum, instances) -> sum + instances, (u, v) -> v);
  }

  @JsonIgnore
  public List<StackGresClusterReplicationGroup> getReplicationGroups() {
    StackGresClusterReplicationGroup implicitGroup = new StackGresClusterReplicationGroup();
    implicitGroup.setRole(Optional.ofNullable(replication)
        .map(StackGresClusterReplication::getRole)
        .orElse(StackGresReplicationRole.HA_READ.toString()));
    implicitGroup.setInstances(getInstancesInImplicitReplicationGroup());
    return ImmutableList.<StackGresClusterReplicationGroup>builder()
        .add(implicitGroup)
        .addAll(Optional.ofNullable(replication)
            .map(StackGresClusterReplication::getGroups)
            .orElse(ImmutableList.of()))
        .build();
  }

  public StackGresClusterPostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(StackGresClusterPostgres postgres) {
    this.postgres = postgres;
  }

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  public StackGresClusterReplication getReplication() {
    return replication;
  }

  public void setReplication(StackGresClusterReplication replication) {
    this.replication = replication;
  }

  public StackGresClusterConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(StackGresClusterConfiguration configuration) {
    this.configuration = configuration;
  }

  public String getResourceProfile() {
    return resourceProfile;
  }

  public void setResourceProfile(String resourceProfile) {
    this.resourceProfile = resourceProfile;
  }

  public StackGresClusterInitData getInitData() {
    return initData;
  }

  public void setInitData(StackGresClusterInitData initData) {
    this.initData = initData;
  }

  public StackGresClusterReplicateFrom getReplicateFrom() {
    return replicateFrom;
  }

  public void setReplicateFrom(StackGresClusterReplicateFrom replicateFrom) {
    this.replicateFrom = replicateFrom;
  }

  public StackGresClusterManagedSql getManagedSql() {
    return managedSql;
  }

  public void setManagedSql(StackGresClusterManagedSql managedSql) {
    this.managedSql = managedSql;
  }

  public StackGresClusterPod getPod() {
    return pod;
  }

  public void setPod(StackGresClusterPod pod) {
    this.pod = pod;
  }

  public StackGresClusterDistributedLogs getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(StackGresClusterDistributedLogs distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  public List<StackGresClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<StackGresClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
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

  public void setNonProductionOptions(StackGresClusterNonProduction nonProduction) {
    this.nonProductionOptions = nonProduction;
  }

  public StackGresClusterPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(StackGresClusterPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
  }

  public StackGresClusterSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(StackGresClusterSpecMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configuration, distributedLogs, initData, instances, managedSql, metadata,
        nonProductionOptions, pod, postgres, postgresServices, prometheusAutobind, replicateFrom,
        replication, resourceProfile, toInstallPostgresExtensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterSpec)) {
      return false;
    }
    StackGresClusterSpec other = (StackGresClusterSpec) obj;
    return Objects.equals(configuration, other.configuration)
        && Objects.equals(distributedLogs, other.distributedLogs)
        && Objects.equals(initData, other.initData) && instances == other.instances
        && Objects.equals(managedSql, other.managedSql) && Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProductionOptions, other.nonProductionOptions)
        && Objects.equals(pod, other.pod) && Objects.equals(postgres, other.postgres)
        && Objects.equals(postgresServices, other.postgresServices)
        && Objects.equals(prometheusAutobind, other.prometheusAutobind)
        && Objects.equals(replicateFrom, other.replicateFrom)
        && Objects.equals(replication, other.replication)
        && Objects.equals(resourceProfile, other.resourceProfile)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
