/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterSpec {

  @JsonProperty("postgres")
  private ClusterPostgres postgres;

  @JsonProperty("instances")
  private int instances;

  @JsonProperty("replication")
  private ClusterReplication replication;

  @JsonProperty("configurations")
  private ClusterConfiguration configurations;

  @JsonProperty("sgInstanceProfile")
  private String sgInstanceProfile;

  @JsonProperty("initialData")
  private ClusterInitData initData;

  @JsonProperty("distributedLogs")
  private ClusterDistributedLogs distributedLogs;

  @JsonProperty("toInstallPostgresExtensions")
  private List<ClusterInstalledExtension> toInstallPostgresExtensions;

  @JsonProperty("pods")
  private ClusterPod pods;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  private ClusterNonProduction nonProductionOptions;

  @JsonProperty("postgresServices")
  private ClusterPostgresServices postgresServices;

  @JsonProperty("metadata")
  private ClusterSpecMetadata metadata;

  public ClusterPostgres getPostgres() {
    return postgres;
  }

  public void setPostgres(ClusterPostgres postgres) {
    this.postgres = postgres;
  }

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  public ClusterReplication getReplication() {
    return replication;
  }

  public void setReplication(ClusterReplication replication) {
    this.replication = replication;
  }

  public ClusterConfiguration getConfigurations() {
    return configurations;
  }

  public void setConfigurations(ClusterConfiguration configurations) {
    this.configurations = configurations;
  }

  public String getSgInstanceProfile() {
    return sgInstanceProfile;
  }

  public void setSgInstanceProfile(String sgInstanceProfile) {
    this.sgInstanceProfile = sgInstanceProfile;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  public ClusterNonProduction getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(ClusterNonProduction nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public ClusterPod getPods() {
    return pods;
  }

  public void setPods(ClusterPod pods) {
    this.pods = pods;
  }

  public ClusterInitData getInitData() {
    return initData;
  }

  public void setInitData(ClusterInitData initData) {
    this.initData = initData;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterSpec that = (ClusterSpec) o;
    return instances == that.instances && Objects.equals(postgres, that.postgres)
        && Objects.equals(configurations, that.configurations)
        && Objects.equals(sgInstanceProfile, that.sgInstanceProfile)
        && Objects.equals(initData, that.initData)
        && Objects.equals(distributedLogs, that.distributedLogs)
        && Objects.equals(toInstallPostgresExtensions, that.toInstallPostgresExtensions)
        && Objects.equals(pods, that.pods)
        && Objects.equals(prometheusAutobind, that.prometheusAutobind)
        && Objects.equals(nonProductionOptions, that.nonProductionOptions)
        && Objects.equals(postgresServices, that.postgresServices)
        && Objects.equals(metadata, that.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postgres, instances, configurations, sgInstanceProfile,
        initData, distributedLogs, toInstallPostgresExtensions, pods, prometheusAutobind,
        nonProductionOptions, postgresServices, metadata);
  }
}
