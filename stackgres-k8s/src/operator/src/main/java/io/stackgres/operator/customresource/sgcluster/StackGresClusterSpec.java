/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgcluster;

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterSpec implements KubernetesResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @JsonProperty("instances")
  @Min(value = 1, message = "You need at least 1 instance in the cluster")
  private int instances;

  @JsonProperty("pgVersion")
  @NotBlank(message = "PostgreSQL version is required")
  private String postgresVersion;

  @JsonProperty("pgConfig")
  @NotBlank(message = "You need to associate a Postgres configuration to this cluster")
  private String postgresConfig;

  @JsonProperty("resourceProfile")
  @NotNull
  private String resourceProfile;

  @JsonProperty("connectionPoolingConfig")
  @NotNull
  private String connectionPoolingConfig;

  @JsonProperty("backupConfig")
  private String backupConfig;

  @JsonProperty("volumeSize")
  @NotNull
  private String volumeSize;

  @JsonProperty("storageClass")
  private String storageClass;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("sidecars")
  private List<String> sidecars;

  @JsonProperty("nonProductionOptions")
  private NonProduction nonProduction;

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

  public String getPostgresConfig() {
    return postgresConfig;
  }

  public void setPostgresConfig(String postgresConfig) {
    this.postgresConfig = postgresConfig;
  }

  public String getResourceProfile() {
    return resourceProfile;
  }

  public void setResourceProfile(String resourceProfile) {
    this.resourceProfile = resourceProfile;
  }

  public String getConnectionPoolingConfig() {
    return connectionPoolingConfig;
  }

  public void setConnectionPoolingConfig(String connectionPoolingConfig) {
    this.connectionPoolingConfig = connectionPoolingConfig;
  }

  public String getBackupConfig() {
    return backupConfig;
  }

  public void setBackupConfig(String backupConfig) {
    this.backupConfig = backupConfig;
  }

  public String getVolumeSize() {
    return volumeSize;
  }

  public void setVolumeSize(String volumeSize) {
    this.volumeSize = volumeSize;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  public List<String> getSidecars() {
    return sidecars;
  }

  public void setSidecars(List<String> sidecars) {
    this.sidecars = sidecars;
  }

  public NonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(NonProduction nonProduction) {
    this.nonProduction = nonProduction;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("instances", instances)
        .add("pgVersion", postgresVersion)
        .add("pgConfig", postgresConfig)
        .add("resourceProfile", resourceProfile)
        .add("connectionPoolingConfig", connectionPoolingConfig)
        .add("backupConfig", backupConfig)
        .add("volumeSize", volumeSize)
        .add("storageClass", storageClass)
        .add("sidecars", sidecars)
        .add("nonProduction", nonProduction)
        .toString();
  }

}
