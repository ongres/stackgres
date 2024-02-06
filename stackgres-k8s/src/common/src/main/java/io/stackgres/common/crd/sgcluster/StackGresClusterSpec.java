/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterSpec {

  @ValidEnum(enumClass = StackGresClusterProfile.class, allowNulls = true,
      message = "profile must be production, testing or development")
  private String profile;

  @Valid
  private StackGresClusterPostgres postgres;

  private Integer instances;

  @Valid
  private StackGresClusterReplication replication;

  @Valid
  private StackGresClusterConfigurations configurations;

  private String sgInstanceProfile;

  @Valid
  private StackGresClusterInitialData initialData;

  @Valid
  private StackGresClusterReplicateFrom replicateFrom;

  @Valid
  private StackGresClusterManagedSql managedSql;

  @Valid
  private StackGresClusterPods pods;

  @Valid
  private StackGresClusterDistributedLogs distributedLogs;

  @Valid
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  private Boolean prometheusAutobind;

  @Valid
  private StackGresClusterNonProduction nonProductionOptions;

  @Valid
  private StackGresClusterPostgresServices postgresServices;

  @Valid
  private StackGresClusterSpecMetadata metadata;

  @ReferencedField("postgres")
  interface Postgres extends FieldReference { }

  @ReferencedField("replication")
  interface Replication extends FieldReference { }

  @ReferencedField("instances")
  interface Instances extends FieldReference { }

  @ReferencedField("sgInstanceProfile")
  interface SgInstanceProfile extends FieldReference { }

  @ReferencedField("postgresServices")
  interface PostgresServices extends FieldReference { }

  @ReferencedField("configurations")
  interface Configurations extends FieldReference { }

  @ReferencedField("pods")
  interface Pods extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "sgInstanceProfile is required", payload = { SgInstanceProfile.class })
  public boolean isResourceProfilePresent() {
    return sgInstanceProfile != null;
  }

  @JsonIgnore
  @AssertTrue(message = "postgres is required", payload = { Postgres.class })
  public boolean isPosgresSectionPresent() {
    return postgres != null;
  }

  @JsonIgnore
  @AssertTrue(message = "configurations is required", payload = { Configurations.class })
  public boolean isConfigurationsSectionPresent() {
    return configurations != null;
  }

  @JsonIgnore
  @AssertTrue(message = "pods is required", payload = { Pods.class })
  public boolean isPodsSectionPresent() {
    return pods != null;
  }

  @JsonIgnore
  @AssertTrue(message = "instances can not be negative",
      payload = { Instances.class })
  public boolean isInstancesPositive() {
    return instances >= 0;
  }

  @JsonIgnore
  @AssertTrue(message = "postgresServices is required",
      payload = { PostgresServices.class })
  public boolean isPostgresServicesPresent() {
    return postgresServices != null;
  }

  @JsonIgnore
  @AssertTrue(message = "replication is required", payload = { Replication.class })
  public boolean isReplicationSectionPresent() {
    return replication != null;
  }

  @JsonIgnore
  @AssertTrue(message = "The total number of instances must be greather than the number of"
      + " instances in replication groups", payload = { Instances.class })
  public boolean isSupportingInstancesForInstancesInReplicationGroups() {
    return instances == 0
        || instances > getInstancesInReplicationGroups();
  }

  @JsonIgnore
  @AssertTrue(message = "The total number of instances must be greather than the number of"
      + " synchronous replicas", payload = { Instances.class })
  public boolean isSupportingRequiredSynchronousReplicas() {
    return replication == null
        || !replication.isSynchronousMode()
        || replication.getSyncInstances() == null
        || instances == 0
        || instances > replication.getSyncInstances();
  }

  @JsonIgnore
  public int getInstancesInImplicitReplicationGroup() {
    return Math.max(0, instances - getInstancesInReplicationGroups());
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
            .stream()
            .flatMap(List::stream)
            .map(group -> new StackGresClusterReplicationGroupBuilder(group)
                .withInstances(instances == 0 ? instances : group.getInstances())
                .build())
            .toList())
        .build();
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public StackGresClusterPostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(StackGresClusterPostgres postgres) {
    this.postgres = postgres;
  }

  public Integer getInstances() {
    return instances;
  }

  public void setInstances(Integer instances) {
    this.instances = instances;
  }

  public StackGresClusterReplication getReplication() {
    return replication;
  }

  public void setReplication(StackGresClusterReplication replication) {
    this.replication = replication;
  }

  public StackGresClusterConfigurations getConfigurations() {
    return configurations;
  }

  public void setConfigurations(StackGresClusterConfigurations configurations) {
    this.configurations = configurations;
  }

  public String getSgInstanceProfile() {
    return sgInstanceProfile;
  }

  public void setSgInstanceProfile(String sgInstanceProfile) {
    this.sgInstanceProfile = sgInstanceProfile;
  }

  public StackGresClusterInitialData getInitialData() {
    return initialData;
  }

  public void setInitialData(StackGresClusterInitialData initialData) {
    this.initialData = initialData;
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

  public StackGresClusterPods getPods() {
    return pods;
  }

  public void setPods(StackGresClusterPods pods) {
    this.pods = pods;
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
    return Objects.hash(configurations, distributedLogs, initialData, instances, managedSql,
        metadata, nonProductionOptions, pods, postgres, postgresServices, profile,
        prometheusAutobind, replicateFrom, replication, sgInstanceProfile,
        toInstallPostgresExtensions);
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
    return Objects.equals(configurations, other.configurations)
        && Objects.equals(distributedLogs, other.distributedLogs)
        && Objects.equals(initialData, other.initialData)
        && Objects.equals(instances, other.instances)
        && Objects.equals(managedSql, other.managedSql)
        && Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProductionOptions, other.nonProductionOptions)
        && Objects.equals(pods, other.pods)
        && Objects.equals(postgres, other.postgres)
        && Objects.equals(postgresServices, other.postgresServices)
        && Objects.equals(profile, other.profile)
        && Objects.equals(prometheusAutobind, other.prometheusAutobind)
        && Objects.equals(replicateFrom, other.replicateFrom)
        && Objects.equals(replication, other.replication)
        && Objects.equals(sgInstanceProfile, other.sgInstanceProfile)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
