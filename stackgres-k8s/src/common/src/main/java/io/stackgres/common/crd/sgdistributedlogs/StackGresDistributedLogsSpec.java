/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDistributedLogsSpec {

  @JsonProperty("persistentVolume")
  @NotNull(message = "Persistent volume must be specified")
  @Valid
  private StackGresDistributedLogsPersistentVolume persistentVolume;

  @JsonProperty("postgresServices")
  @Valid
  private StackGresDistributedLogsPostgresServices postgresServices;

  @JsonProperty("nonProductionOptions")
  @Valid
  private StackGresDistributedLogsNonProduction nonProductionOptions;

  @JsonProperty("resources")
  private StackGresDistributedLogsResources resources;

  @JsonProperty("scheduling")
  @Valid
  private StackGresDistributedLogsPodScheduling scheduling;

  @JsonProperty("sgInstanceProfile")
  @NotNull(message = "resource profile is required")
  private String resourceProfile;

  @JsonProperty("configurations")
  @NotNull(message = "configurations section is required")
  @Valid
  private StackGresDistributedLogsConfiguration configuration;

  @JsonProperty("metadata")
  @Valid
  private StackGresDistributedLogsSpecMetadata metadata;

  @JsonProperty("toInstallPostgresExtensions")
  @Valid
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  public StackGresDistributedLogsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      StackGresDistributedLogsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public StackGresDistributedLogsNonProduction getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(StackGresDistributedLogsNonProduction nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public StackGresDistributedLogsResources getResources() {
    return resources;
  }

  public void setResources(StackGresDistributedLogsResources resources) {
    this.resources = resources;
  }

  public StackGresDistributedLogsPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresDistributedLogsPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public String getResourceProfile() {
    return resourceProfile;
  }

  public void setResourceProfile(String resourceProfile) {
    this.resourceProfile = resourceProfile;
  }

  public StackGresDistributedLogsConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(StackGresDistributedLogsConfiguration configuration) {
    this.configuration = configuration;
  }

  public StackGresDistributedLogsSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(StackGresDistributedLogsSpecMetadata metadata) {
    this.metadata = metadata;
  }

  public List<StackGresClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<StackGresClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  public StackGresDistributedLogsPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(StackGresDistributedLogsPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configuration, metadata, nonProductionOptions, persistentVolume,
        postgresServices, resourceProfile, scheduling, toInstallPostgresExtensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsSpec)) {
      return false;
    }
    StackGresDistributedLogsSpec other = (StackGresDistributedLogsSpec) obj;
    return Objects.equals(configuration, other.configuration)
        && Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProductionOptions, other.nonProductionOptions)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(postgresServices, other.postgresServices)
        && Objects.equals(resourceProfile, other.resourceProfile)
        && Objects.equals(scheduling, other.scheduling)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
