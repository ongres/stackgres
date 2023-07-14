/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomContainer;
import io.stackgres.common.crd.CustomVolume;
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
public class StackGresClusterPod {

  @JsonProperty("persistentVolume")
  @Valid
  private StackGresPodPersistentVolume persistentVolume;

  @JsonProperty("disableConnectionPooling")
  private Boolean disableConnectionPooling;

  @JsonProperty("disableMetricsExporter")
  private Boolean disableMetricsExporter;

  @JsonProperty("disablePostgresUtil")
  private Boolean disablePostgresUtil;

  @JsonProperty("managementPolicy")
  @ValidEnum(enumClass = StackGresPodManagementPolicy.class, allowNulls = true,
      message = "managementPolicy must be OrderedReady or Parallel")
  private String managementPolicy;

  @JsonProperty("resources")
  @Valid
  private StackGresClusterResources resources;

  @JsonProperty("scheduling")
  @Valid
  private StackGresClusterPodScheduling scheduling;

  @JsonProperty("customVolumes")
  @Valid
  private List<CustomVolume> customVolumes;

  @JsonProperty("customContainers")
  @Valid
  private List<CustomContainer> customContainers;

  @JsonProperty("customInitContainers")
  @Valid
  private List<CustomContainer> customInitContainers;

  @ReferencedField("persistentVolume")
  interface PersistentVolume extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "persistentVolume is required",
      payload = { PersistentVolume.class })
  public boolean isPersistentVolumeSectionPresent() {
    return persistentVolume != null;
  }

  public StackGresPodPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresPodPersistentVolume persistentVolume) {
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

  public StackGresClusterResources getResources() {
    return resources;
  }

  public void setResources(StackGresClusterResources resources) {
    this.resources = resources;
  }

  public StackGresClusterPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresClusterPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public String getManagementPolicy() {
    return managementPolicy;
  }

  public void setManagementPolicy(String managementPolicy) {
    this.managementPolicy = managementPolicy;
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
  public int hashCode() {
    return Objects.hash(customContainers, customInitContainers, customVolumes,
        disableConnectionPooling, disableMetricsExporter, disablePostgresUtil, managementPolicy,
        persistentVolume, resources, scheduling);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPod)) {
      return false;
    }
    StackGresClusterPod other = (StackGresClusterPod) obj;
    return Objects.equals(customContainers, other.customContainers)
        && Objects.equals(customInitContainers, other.customInitContainers)
        && Objects.equals(customVolumes, other.customVolumes)
        && Objects.equals(disableConnectionPooling, other.disableConnectionPooling)
        && Objects.equals(disableMetricsExporter, other.disableMetricsExporter)
        && Objects.equals(disablePostgresUtil, other.disablePostgresUtil)
        && Objects.equals(managementPolicy, other.managementPolicy)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(resources, other.resources)
        && Objects.equals(scheduling, other.scheduling);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
