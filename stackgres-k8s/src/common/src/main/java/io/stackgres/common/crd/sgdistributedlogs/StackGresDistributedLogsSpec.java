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
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDistributedLogsSpec {

  @NotNull(message = "Persistent volume must be specified")
  @Valid
  private StackGresDistributedLogsPersistentVolume persistentVolume;

  @Valid
  private StackGresDistributedLogsPostgresServices postgresServices;

  @Valid
  private StackGresDistributedLogsNonProduction nonProductionOptions;

  private StackGresDistributedLogsResources resources;

  @Valid
  private StackGresDistributedLogsPodScheduling scheduling;

  @NotNull(message = "resource profile is required")
  private String sgInstanceProfile;

  @NotNull(message = "configurations is required")
  @Valid
  private StackGresDistributedLogsConfigurations configurations;

  @Valid
  private StackGresDistributedLogsSpecMetadata metadata;

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

  public String getSgInstanceProfile() {
    return sgInstanceProfile;
  }

  public void setSgInstanceProfile(String sgInstanceProfile) {
    this.sgInstanceProfile = sgInstanceProfile;
  }

  public StackGresDistributedLogsConfigurations getConfigurations() {
    return configurations;
  }

  public void setConfigurations(StackGresDistributedLogsConfigurations configurations) {
    this.configurations = configurations;
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
    return Objects.hash(configurations, metadata, nonProductionOptions, persistentVolume,
        postgresServices, sgInstanceProfile, scheduling, toInstallPostgresExtensions);
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
    return Objects.equals(configurations, other.configurations)
        && Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProductionOptions, other.nonProductionOptions)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(postgresServices, other.postgresServices)
        && Objects.equals(sgInstanceProfile, other.sgInstanceProfile)
        && Objects.equals(scheduling, other.scheduling)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
