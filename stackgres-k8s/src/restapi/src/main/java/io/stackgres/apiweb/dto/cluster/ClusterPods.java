/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Probe;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomContainer;
import io.stackgres.common.crd.CustomEnvFromSource;
import io.stackgres.common.crd.CustomEnvVar;
import io.stackgres.common.crd.CustomVolume;
import io.stackgres.common.crd.CustomVolumeMount;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
//TODO remove once the UI has fixes the sending metadata in this object
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterPods {

  private ClusterPodsPersistentVolume persistentVolume;

  private Boolean disableConnectionPooling;

  private Boolean disableMetricsExporter;

  private Boolean disablePostgresUtil;

  private Boolean disableEnvoy;

  private String managementPolicy;

  private ClusterResources resources;

  private ClusterPodsScheduling scheduling;

  private List<CustomVolume> customVolumes;

  private List<CustomContainer> customContainers;

  private List<CustomContainer> customInitContainers;

  private Map<String, List<CustomVolumeMount>> customVolumeMounts;

  private Map<String, List<CustomVolumeMount>> customInitVolumeMounts;

  private Map<String, List<CustomEnvVar>> customEnv;

  private Map<String, List<CustomEnvVar>> customInitEnv;

  private Map<String, List<CustomEnvFromSource>> customEnvFrom;

  private Map<String, List<CustomEnvFromSource>> customInitEnvFrom;

  private Long terminationGracePeriodSeconds;

  private Probe readinessProbe;

  private Probe livenessProbe;

  public ClusterPodsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(ClusterPodsPersistentVolume persistentVolume) {
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

  public Boolean getDisableEnvoy() {
    return disableEnvoy;
  }

  public void setDisableEnvoy(Boolean disableEnvoy) {
    this.disableEnvoy = disableEnvoy;
  }

  public String getManagementPolicy() {
    return managementPolicy;
  }

  public void setManagementPolicy(String managementPolicy) {
    this.managementPolicy = managementPolicy;
  }

  public ClusterResources getResources() {
    return resources;
  }

  public void setResources(ClusterResources resources) {
    this.resources = resources;
  }

  public ClusterPodsScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(ClusterPodsScheduling scheduling) {
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

  public Map<String, List<CustomVolumeMount>> getCustomVolumeMounts() {
    return customVolumeMounts;
  }

  public void setCustomVolumeMounts(Map<String, List<CustomVolumeMount>> customVolumeMounts) {
    this.customVolumeMounts = customVolumeMounts;
  }

  public Map<String, List<CustomVolumeMount>> getCustomInitVolumeMounts() {
    return customInitVolumeMounts;
  }

  public void setCustomInitVolumeMounts(Map<String, List<CustomVolumeMount>> customInitVolumeMounts) {
    this.customInitVolumeMounts = customInitVolumeMounts;
  }

  public Map<String, List<CustomEnvVar>> getCustomEnv() {
    return customEnv;
  }

  public void setCustomEnv(Map<String, List<CustomEnvVar>> customEnv) {
    this.customEnv = customEnv;
  }

  public Map<String, List<CustomEnvVar>> getCustomInitEnv() {
    return customInitEnv;
  }

  public void setCustomInitEnv(Map<String, List<CustomEnvVar>> customInitEnv) {
    this.customInitEnv = customInitEnv;
  }

  public Map<String, List<CustomEnvFromSource>> getCustomEnvFrom() {
    return customEnvFrom;
  }

  public void setCustomEnvFrom(Map<String, List<CustomEnvFromSource>> customEnvFrom) {
    this.customEnvFrom = customEnvFrom;
  }

  public Map<String, List<CustomEnvFromSource>> getCustomInitEnvFrom() {
    return customInitEnvFrom;
  }

  public void setCustomInitEnvFrom(
      Map<String, List<CustomEnvFromSource>> customInitEnvFrom) {
    this.customInitEnvFrom = customInitEnvFrom;
  }

  public Long getTerminationGracePeriodSeconds() {
    return terminationGracePeriodSeconds;
  }

  public void setTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {
    this.terminationGracePeriodSeconds = terminationGracePeriodSeconds;
  }

  public Probe getReadinessProbe() {
    return readinessProbe;
  }

  public void setReadinessProbe(Probe readinessProbe) {
    this.readinessProbe = readinessProbe;
  }

  public Probe getLivenessProbe() {
    return livenessProbe;
  }

  public void setLivenessProbe(Probe livenessProbe) {
    this.livenessProbe = livenessProbe;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
