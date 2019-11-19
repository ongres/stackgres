/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ClusterSpec {

  @JsonProperty("instances")
  private int instances;

  @JsonProperty("pgVersion")
  private String postgresVersion;

  @JsonProperty("pgConfig")
  private String postgresConfig;

  @JsonProperty("postgresExporterVersion")
  private String postgresExporterVersion;

  @JsonProperty("envoyVersion")
  private String envoyVersion;

  @JsonProperty("resourceProfile")
  private String resourceProfile;

  @JsonProperty("connectionPoolingConfig")
  private String connectionPoolingConfig;

  @JsonProperty("volumeSize")
  private String volumeSize;

  @JsonProperty("storageClass")
  private String storageClass;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("sidecars")
  private List<String> sidecars;

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

  public String getPostgresExporterVersion() {
    return postgresExporterVersion;
  }

  public void setPostgresExporterVersion(String postgresExporterVersion) {
    this.postgresExporterVersion = postgresExporterVersion;
  }

  public String getEnvoyVersion() {
    return envoyVersion;
  }

  public void setEnvoyVersion(String envoyVersion) {
    this.envoyVersion = envoyVersion;
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
}
