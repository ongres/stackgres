/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.autoscaling;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class VerticalPodAutoscalerContainerPolicy {

  private String containerName;

  private List<String> controlledResources;

  private String controlledValues;

  private Map<String, String> minAllowed;

  private Map<String, String> maxAllowed;

  private String mode;

  public String getContainerName() {
    return containerName;
  }

  public void setContainerName(String containerName) {
    this.containerName = containerName;
  }

  public List<String> getControlledResources() {
    return controlledResources;
  }

  public void setControlledResources(List<String> controlledResources) {
    this.controlledResources = controlledResources;
  }

  public String getControlledValues() {
    return controlledValues;
  }

  public void setControlledValues(String controlledValues) {
    this.controlledValues = controlledValues;
  }

  public Map<String, String> getMinAllowed() {
    return minAllowed;
  }

  public void setMinAllowed(Map<String, String> minAllowed) {
    this.minAllowed = minAllowed;
  }

  public Map<String, String> getMaxAllowed() {
    return maxAllowed;
  }

  public void setMaxAllowed(Map<String, String> maxAllowed) {
    this.maxAllowed = maxAllowed;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(containerName, controlledResources, controlledValues, maxAllowed, minAllowed, mode);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VerticalPodAutoscalerContainerPolicy)) {
      return false;
    }
    VerticalPodAutoscalerContainerPolicy other = (VerticalPodAutoscalerContainerPolicy) obj;
    return Objects.equals(containerName, other.containerName)
        && Objects.equals(controlledResources, other.controlledResources)
        && Objects.equals(controlledValues, other.controlledValues) && Objects.equals(maxAllowed, other.maxAllowed)
        && Objects.equals(minAllowed, other.minAllowed) && Objects.equals(mode, other.mode);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
