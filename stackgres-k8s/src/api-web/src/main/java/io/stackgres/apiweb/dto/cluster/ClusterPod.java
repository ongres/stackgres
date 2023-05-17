/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomContainer;
import io.stackgres.common.crd.CustomVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
//TODO remove once the UI has fixes the sending metadata in this object
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterPod {

  @JsonProperty("persistentVolume")
  private ClusterPodPersistentVolume persistentVolume;

  @JsonProperty("disableConnectionPooling")
  private Boolean disableConnectionPooling;

  @JsonProperty("disableMetricsExporter")
  private Boolean disableMetricsExporter;

  @JsonProperty("disablePostgresUtil")
  private Boolean disablePostgresUtil;

  @JsonProperty("managementPolicy")
  private String managementPolicy;

  @JsonProperty("resources")
  private StackGresClusterResources resources;

  @JsonProperty("scheduling")
  private ClusterPodScheduling scheduling;

  @JsonProperty("customVolumes")
  private List<CustomVolume> customVolumes;

  @JsonProperty("customContainers")
  private List<CustomContainer> customContainers;

  @JsonProperty("customInitContainers")
  private List<CustomContainer> customInitContainers;

  public ClusterPodPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(ClusterPodPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public Boolean getDisableConnectionPooling() {
    return disableConnectionPooling;
  }

  public void setDisableConnectionPooling(Boolean disableConnectionPooling) {
    this.disableConnectionPooling = disableConnectionPooling;
  }

  public Boolean getDisableMetricsExporter() {
    return disableMetricsExporter;
  }

  public void setDisableMetricsExporter(Boolean disableMetricsExporter) {
    this.disableMetricsExporter = disableMetricsExporter;
  }

  public Boolean getDisablePostgresUtil() {
    return disablePostgresUtil;
  }

  public void setDisablePostgresUtil(Boolean disablePostgresUtil) {
    this.disablePostgresUtil = disablePostgresUtil;
  }

  public String getManagementPolicy() {
    return managementPolicy;
  }

  public void setManagementPolicy(String managementPolicy) {
    this.managementPolicy = managementPolicy;
  }

  public StackGresClusterResources getResources() {
    return resources;
  }

  public void setResources(StackGresClusterResources resources) {
    this.resources = resources;
  }

  public ClusterPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(ClusterPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public List<CustomVolume> getCustomVolumes() {
    return customVolumes;
  }

  public void setCustomVolumes(List<CustomVolume> customVolumes) {
    this.customVolumes = customVolumes;
  }

  public List<CustomContainer> getCustomContainers() {
    return customContainers;
  }

  public void setCustomContainers(List<CustomContainer> customContainers) {
    this.customContainers = customContainers;
  }

  public List<CustomContainer> getCustomInitContainers() {
    return customInitContainers;
  }

  public void setCustomInitContainers(List<CustomContainer> customInitContainers) {
    this.customInitContainers = customInitContainers;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
