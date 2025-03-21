/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterSpec {

  private String profile;

  private ClusterPostgres postgres;

  private Integer instances;

  private ClusterAutoscaling autoscaling;

  private ClusterReplication replication;

  private ClusterConfigurations configurations;

  private String sgInstanceProfile;

  private ClusterInitialData initialData;

  private ClusterReplicateFrom replicateFrom;

  private ClusterManagedSql managedSql;

  private ClusterDistributedLogs distributedLogs;

  private List<ClusterInstalledExtension> toInstallPostgresExtensions;

  private ClusterPods pods;

  private ClusterNonProductionOptions nonProductionOptions;

  private ClusterPostgresServices postgresServices;

  private ClusterSpecMetadata metadata;

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public ClusterPostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(ClusterPostgres postgres) {
    this.postgres = postgres;
  }

  public Integer getInstances() {
    return instances;
  }

  public void setInstances(Integer instances) {
    this.instances = instances;
  }

  public ClusterAutoscaling getAutoscaling() {
    return autoscaling;
  }

  public void setAutoscaling(ClusterAutoscaling autoscaling) {
    this.autoscaling = autoscaling;
  }

  public ClusterReplication getReplication() {
    return replication;
  }

  public void setReplication(ClusterReplication replication) {
    this.replication = replication;
  }

  public ClusterConfigurations getConfigurations() {
    return configurations;
  }

  public void setConfigurations(ClusterConfigurations configurations) {
    this.configurations = configurations;
  }

  public String getSgInstanceProfile() {
    return sgInstanceProfile;
  }

  public void setSgInstanceProfile(String sgInstanceProfile) {
    this.sgInstanceProfile = sgInstanceProfile;
  }

  public ClusterNonProductionOptions getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(ClusterNonProductionOptions nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public ClusterPods getPods() {
    return pods;
  }

  public void setPods(ClusterPods pods) {
    this.pods = pods;
  }

  public ClusterInitialData getInitialData() {
    return initialData;
  }

  public void setInitialData(ClusterInitialData initialData) {
    this.initialData = initialData;
  }

  public ClusterReplicateFrom getReplicateFrom() {
    return replicateFrom;
  }

  public void setReplicateFrom(ClusterReplicateFrom replicateFrom) {
    this.replicateFrom = replicateFrom;
  }

  public ClusterManagedSql getManagedSql() {
    return managedSql;
  }

  public void setManagedSql(ClusterManagedSql managedSql) {
    this.managedSql = managedSql;
  }

  public ClusterDistributedLogs getDistributedLogs() {
    return distributedLogs;
  }

  public void setDistributedLogs(ClusterDistributedLogs distributedLogs) {
    this.distributedLogs = distributedLogs;
  }

  public List<ClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<ClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  public ClusterPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(ClusterPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
  }

  public ClusterSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ClusterSpecMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
