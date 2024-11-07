/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterProfile;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDistributedLogsSpec {

  @ValidEnum(enumClass = StackGresClusterProfile.class, allowNulls = true,
      message = "profile must be production, testing or development")
  private String profile;

  @NotNull(message = "Persistent volume must be specified")
  @Valid
  private StackGresClusterPodsPersistentVolume persistentVolume;

  @Valid
  private StackGresPostgresServices postgresServices;

  @Valid
  private StackGresClusterNonProduction nonProductionOptions;

  @Valid
  private StackGresClusterResources resources;

  @Valid
  private StackGresClusterPodsScheduling scheduling;

  @NotNull(message = "resource profile is required")
  private String sgInstanceProfile;

  @NotNull(message = "configurations is required")
  @Valid
  private StackGresDistributedLogsConfigurations configurations;

  @Valid
  private StackGresClusterSpecMetadata metadata;

  @Valid
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public StackGresClusterPodsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      StackGresClusterPodsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public StackGresClusterNonProduction getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(StackGresClusterNonProduction nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public StackGresClusterResources getResources() {
    return resources;
  }

  public void setResources(StackGresClusterResources resources) {
    this.resources = resources;
  }

  public StackGresClusterPodsScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresClusterPodsScheduling scheduling) {
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

  public StackGresClusterSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(StackGresClusterSpecMetadata metadata) {
    this.metadata = metadata;
  }

  public List<StackGresClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<StackGresClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  public StackGresPostgresServices getPostgresServices() {
    return postgresServices;
  }

  public void setPostgresServices(StackGresPostgresServices postgresServices) {
    this.postgresServices = postgresServices;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configurations, metadata, nonProductionOptions, persistentVolume,
        postgresServices, profile, resources, scheduling, sgInstanceProfile,
        toInstallPostgresExtensions);
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
        && Objects.equals(profile, other.profile)
        && Objects.equals(resources, other.resources)
        && Objects.equals(scheduling, other.scheduling)
        && Objects.equals(sgInstanceProfile, other.sgInstanceProfile)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
