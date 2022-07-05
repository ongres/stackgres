/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDistributedLogsSpec implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("persistentVolume")
  @NotNull(message = "Persistent volume must be specified")
  @Valid
  private StackGresDistributedLogsPersistentVolume persistentVolume;

  @JsonProperty("postgresServices")
  @Valid
  private StackGresDistributedLogsPostgresServices postgresServices;

  @JsonProperty("nonProductionOptions")
  @Valid
  private StackGresDistributedLogsNonProduction nonProduction;

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

  public StackGresDistributedLogsNonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(StackGresDistributedLogsNonProduction nonProduction) {
    this.nonProduction = nonProduction;
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
    return Objects.hash(configuration, metadata, nonProduction, persistentVolume, postgresServices,
        resourceProfile, scheduling, toInstallPostgresExtensions);
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
        && Objects.equals(nonProduction, other.nonProduction)
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
