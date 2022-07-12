/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DistributedLogsSpec {

  @JsonProperty("persistentVolume")
  private DistributedLogsPersistentVolume persistentVolume;

  @JsonProperty("postgresServices")
  private DistributedLogsPostgresServices postgresServices;

  @JsonProperty("nonProductionOptions")
  private DistributedLogsNonProduction nonProduction;

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
  public int hashCode() {
    return Objects.hash(metadata, nonProduction, persistentVolume, postgresServices, scheduling,
        sgInstanceProfile, toInstallPostgresExtensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DistributedLogsSpec)) {
      return false;
    }
    DistributedLogsSpec other = (DistributedLogsSpec) obj;
    return Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProduction, other.nonProduction)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(postgresServices, other.postgresServices)
        && Objects.equals(scheduling, other.scheduling)
        && Objects.equals(sgInstanceProfile, other.sgInstanceProfile)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
