/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterAutoscaling {

  private String mode;

  private Integer minInstances;

  private Integer maxInstances;

  private Map<String, ClusterAutoscalingVerticalBound> minAllowed;

  private Map<String, ClusterAutoscalingVerticalBound> maxAllowed;

  private ClusterAutoscalingHorizontal horizontal;

  private ClusterAutoscalingVertical vertical;

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public Integer getMinInstances() {
    return minInstances;
  }

  public void setMinInstances(Integer minInstances) {
    this.minInstances = minInstances;
  }

  public Integer getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(Integer maxInstances) {
    this.maxInstances = maxInstances;
  }

  public Map<String, ClusterAutoscalingVerticalBound> getMinAllowed() {
    return minAllowed;
  }

  public void setMinAllowed(Map<String, ClusterAutoscalingVerticalBound> minAllowed) {
    this.minAllowed = minAllowed;
  }

  public Map<String, ClusterAutoscalingVerticalBound> getMaxAllowed() {
    return maxAllowed;
  }

  public void setMaxAllowed(Map<String, ClusterAutoscalingVerticalBound> maxAllowed) {
    this.maxAllowed = maxAllowed;
  }

  public ClusterAutoscalingHorizontal getHorizontal() {
    return horizontal;
  }

  public void setHorizontal(ClusterAutoscalingHorizontal horizontal) {
    this.horizontal = horizontal;
  }

  public ClusterAutoscalingVertical getVertical() {
    return vertical;
  }

  public void setVertical(ClusterAutoscalingVertical vertical) {
    this.vertical = vertical;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
