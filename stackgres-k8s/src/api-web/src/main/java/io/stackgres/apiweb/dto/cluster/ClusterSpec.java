/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterSpec {

  @JsonProperty("instances")
  @Min(value = 1, message = "You need at least 1 instance in the cluster")
  private int instances;

  @JsonProperty("postgresVersion")
  @NotBlank(message = "PostgreSQL version is required")
  private String postgresVersion;

  @JsonProperty("configurations")
  @NotNull(message = "cluster configuration cannot be null")
  @Valid
  private ClusterConfiguration configurations;

  @JsonProperty("sgInstanceProfile")
  @NotNull(message = "resource profile must not be null")
  private String sgInstanceProfile;

  @JsonProperty("initialData")
  private ClusterInitData initData;

  @JsonProperty("distributedLogs")
  private ClusterDistributedLogs distributedLogs;

  @JsonProperty("pods")
  @Valid
  @NotNull(message = "pod description must be specified")
  private ClusterPod pods;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("nonProductionOptions")
  private ClusterNonProduction nonProduction;

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
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

  public ClusterNonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(ClusterNonProduction nonProduction) {
    this.nonProduction = nonProduction;
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("instances", instances)
        .add("pgVersion", postgresVersion)
        .add("configuration", getConfigurations())
        .add("resourceProfile", sgInstanceProfile)
        .add("initData", getInitData())
        .add("distributedLogs", getDistributedLogs())
        .add("pod", getPods())
        .add("nonProductionOptions", nonProduction)
        .toString();
  }
}
