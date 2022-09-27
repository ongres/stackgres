/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DistributedLogsSpec {

  @JsonProperty("persistentVolume")
  private DistributedLogsPersistentVolume persistentVolume;

  @JsonProperty("postgresServices")
  private DistributedLogsPostgresServices postgresServices;

  @JsonProperty("nonProductionOptions")
  private DistributedLogsNonProduction nonProduction;

  @JsonProperty("resources")
  private DistributedLogsResources resources;

  @JsonProperty("scheduling")
  private DistributedLogsPodScheduling scheduling;

  @JsonProperty("sgInstanceProfile")
  private String sgInstanceProfile;

  @JsonProperty("configurations")
  private DistributedLogsConfiguration configuration;

  @JsonProperty("metadata")
  private DistributedLogsSpecMetadata metadata;

  @JsonProperty("toInstallPostgresExtensions")
  private List<ClusterInstalledExtension> toInstallPostgresExtensions;

  public DistributedLogsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      DistributedLogsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public DistributedLogsNonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(DistributedLogsNonProduction nonProduction) {
    this.nonProduction = nonProduction;
  }

  public DistributedLogsResources getResources() {
    return resources;
  }

  public void setResources(DistributedLogsResources resources) {
    this.resources = resources;
  }

  public DistributedLogsPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(DistributedLogsPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public String getSgInstanceProfile() {
    return sgInstanceProfile;
  }

  public void setSgInstanceProfile(String sgInstanceProfile) {
    this.sgInstanceProfile = sgInstanceProfile;
  }

  public DistributedLogsConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(DistributedLogsConfiguration configuration) {
    this.configuration = configuration;
  }

  public DistributedLogsSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(DistributedLogsSpecMetadata metadata) {
    this.metadata = metadata;
  }

  public List<ClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<ClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  public DistributedLogsPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(DistributedLogsPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
