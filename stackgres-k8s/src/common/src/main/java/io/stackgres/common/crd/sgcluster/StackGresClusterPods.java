/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterPods {

  @Valid
  private StackGresClusterPodsPersistentVolume persistentVolume;

  private Boolean disableConnectionPooling;

  private Boolean disableMetricsExporter;

  private Boolean disablePostgresUtil;

  private Boolean disableEnvoy;

  @ValidEnum(enumClass = StackGresClusterManagementPolicy.class, allowNulls = true,
      message = "managementPolicy must be OrderedReady or Parallel")
  private String managementPolicy;

  @Valid
  private StackGresClusterUpdateStrategy updateStrategy;

  @Valid
  private StackGresClusterResources resources;

  @Valid
  private StackGresClusterPodsScheduling scheduling;

  @Valid
  private List<CustomVolume> customVolumes;

  @Valid
  private List<CustomContainer> customContainers;

  @Valid
  private List<CustomContainer> customInitContainers;

  @Valid
  private Map<String, List<CustomVolumeMount>> customVolumeMounts;

  @Valid
  private Map<String, List<CustomVolumeMount>> customInitVolumeMounts;

  @Valid
  private Map<String, List<CustomEnvVar>> customEnv;

  @Valid
  private Map<String, List<CustomEnvVar>> customInitEnv;

  @Valid
  private Map<String, List<CustomEnvFromSource>> customEnvFrom;

  @Valid
  private Map<String, List<CustomEnvFromSource>> customInitEnvFrom;

  private Long terminationGracePeriodSeconds;

  private Probe readinessProbe;

  private Probe livenessProbe;
  
  @ReferencedField("persistentVolume")
  interface PersistentVolume extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "persistentVolume is required",
      payload = { PersistentVolume.class })
  public boolean isPersistentVolumeSectionPresent() {
    return persistentVolume != null;
  }

  public StackGresClusterPodsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresClusterPodsPersistentVolume persistentVolume) {
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

  public String getManagementPolicy() {
    return managementPolicy;
  }

  public void setManagementPolicy(String managementPolicy) {
    this.managementPolicy = managementPolicy;
  }

  public StackGresClusterUpdateStrategy getUpdateStrategy() {
    return updateStrategy;
  }

  public void setUpdateStrategy(StackGresClusterUpdateStrategy updateStrategy) {
    this.updateStrategy = updateStrategy;
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
  public int hashCode() {
    return Objects.hash(customContainers, customEnv, customEnvFrom, customInitContainers,
        customInitEnv, customInitEnvFrom, customInitVolumeMounts, customVolumeMounts, customVolumes,
        disableConnectionPooling, disableEnvoy, disableMetricsExporter, disablePostgresUtil,
        livenessProbe, managementPolicy, persistentVolume, readinessProbe, resources, scheduling,
        terminationGracePeriodSeconds, updateStrategy);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPods)) {
      return false;
    }
    StackGresClusterPods other = (StackGresClusterPods) obj;
    return Objects.equals(customContainers, other.customContainers)
        && Objects.equals(customEnv, other.customEnv)
        && Objects.equals(customEnvFrom, other.customEnvFrom)
        && Objects.equals(customInitContainers, other.customInitContainers)
        && Objects.equals(customInitEnv, other.customInitEnv)
        && Objects.equals(customInitEnvFrom, other.customInitEnvFrom)
        && Objects.equals(customInitVolumeMounts, other.customInitVolumeMounts)
        && Objects.equals(customVolumeMounts, other.customVolumeMounts)
        && Objects.equals(customVolumes, other.customVolumes)
        && Objects.equals(disableConnectionPooling, other.disableConnectionPooling)
        && Objects.equals(disableEnvoy, other.disableEnvoy)
        && Objects.equals(disableMetricsExporter, other.disableMetricsExporter)
        && Objects.equals(disablePostgresUtil, other.disablePostgresUtil)
        && Objects.equals(livenessProbe, other.livenessProbe)
        && Objects.equals(managementPolicy, other.managementPolicy)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(readinessProbe, other.readinessProbe)
        && Objects.equals(resources, other.resources)
        && Objects.equals(scheduling, other.scheduling)
        && Objects.equals(terminationGracePeriodSeconds, other.terminationGracePeriodSeconds)
        && Objects.equals(updateStrategy, other.updateStrategy);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
