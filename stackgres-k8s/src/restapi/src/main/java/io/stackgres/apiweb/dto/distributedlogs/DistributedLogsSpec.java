/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.apiweb.dto.cluster.ClusterResources;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DistributedLogsSpec {

  private String profile;

  private DistributedLogsPersistentVolume persistentVolume;

  private DistributedLogsPostgresServices postgresServices;

  private DistributedLogsNonProductionOptions nonProductionOptions;

  private ClusterResources resources;

  private DistributedLogsPodScheduling scheduling;

  private String sgInstanceProfile;

  private DistributedLogsConfigurations configurations;

  private DistributedLogsSpecMetadata metadata;

  private List<ClusterInstalledExtension> toInstallPostgresExtensions;

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public DistributedLogsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      DistributedLogsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public DistributedLogsNonProductionOptions getNonProductionOptions() {
    return nonProductionOptions;
  }

  public void setNonProductionOptions(DistributedLogsNonProductionOptions nonProductionOptions) {
    this.nonProductionOptions = nonProductionOptions;
  }

  public ClusterResources getResources() {
    return resources;
  }

  public void setResources(ClusterResources resources) {
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

  public DistributedLogsConfigurations getConfigurations() {
    return configurations;
  }

  public void setConfigurations(DistributedLogsConfigurations configurations) {
    this.configurations = configurations;
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
