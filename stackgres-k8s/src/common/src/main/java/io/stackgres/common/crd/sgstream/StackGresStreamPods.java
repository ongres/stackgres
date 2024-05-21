/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamPods {

  @Valid
  private ResourceRequirements resources;

  @Valid
  private StackGresStreamPodsScheduling scheduling;

  @Valid
  private StackGresStreamPodsPersistentVolume persistentVolume;

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }

  public StackGresStreamPodsScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresStreamPodsScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public StackGresStreamPodsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresStreamPodsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  @Override
  public int hashCode() {
    return Objects.hash(persistentVolume, resources, scheduling);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamPods)) {
      return false;
    }
    StackGresStreamPods other = (StackGresStreamPods) obj;
    return Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(resources, other.resources)
        && Objects.equals(scheduling, other.scheduling);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
