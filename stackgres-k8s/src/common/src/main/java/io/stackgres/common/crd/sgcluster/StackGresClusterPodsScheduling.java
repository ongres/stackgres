/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.PodAffinity;
import io.stackgres.common.crd.PodAntiAffinity;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.crd.TopologySpreadConstraint;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterPodsScheduling {

  private Map<String, String> nodeSelector;

  @Valid
  private List<Toleration> tolerations;

  @Valid
  private NodeAffinity nodeAffinity;

  private String preemptionPolicy;

  private String priorityClassName;

  private String runtimeClassName;

  private String schedulerName;

  @Valid
  private PodAffinity podAffinity;

  @Valid
  private PodAntiAffinity podAntiAffinity;

  @Valid
  private List<TopologySpreadConstraint> topologySpreadConstraints;

  @Valid
  private StackGresClusterPodsSchedulingBackup backup;

  public Map<String, String> getNodeSelector() {
    return nodeSelector;
  }

  public void setNodeSelector(Map<String, String> nodeSelector) {
    this.nodeSelector = nodeSelector;
  }

  public List<Toleration> getTolerations() {
    return tolerations;
  }

  public void setTolerations(List<Toleration> tolerations) {
    this.tolerations = tolerations;
  }

  public NodeAffinity getNodeAffinity() {
    return nodeAffinity;
  }

  public void setNodeAffinity(NodeAffinity nodeAffinity) {
    this.nodeAffinity = nodeAffinity;
  }

  public String getPreemptionPolicy() {
    return preemptionPolicy;
  }

  public void setPreemptionPolicy(String preemptionPolicy) {
    this.preemptionPolicy = preemptionPolicy;
  }

  public String getPriorityClassName() {
    return priorityClassName;
  }

  public void setPriorityClassName(String priorityClassName) {
    this.priorityClassName = priorityClassName;
  }

  public String getRuntimeClassName() {
    return runtimeClassName;
  }

  public void setRuntimeClassName(String runtimeClassName) {
    this.runtimeClassName = runtimeClassName;
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }

  public PodAffinity getPodAffinity() {
    return podAffinity;
  }

  public void setPodAffinity(PodAffinity podAffinity) {
    this.podAffinity = podAffinity;
  }

  public PodAntiAffinity getPodAntiAffinity() {
    return podAntiAffinity;
  }

  public void setPodAntiAffinity(PodAntiAffinity podAntiAffinity) {
    this.podAntiAffinity = podAntiAffinity;
  }

  public List<TopologySpreadConstraint> getTopologySpreadConstraints() {
    return topologySpreadConstraints;
  }

  public void setTopologySpreadConstraints(
      List<TopologySpreadConstraint> topologySpreadConstraints) {
    this.topologySpreadConstraints = topologySpreadConstraints;
  }

  public StackGresClusterPodsSchedulingBackup getBackup() {
    return backup;
  }

  public void setBackup(StackGresClusterPodsSchedulingBackup backup) {
    this.backup = backup;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPodsScheduling)) {
      return false;
    }
    StackGresClusterPodsScheduling other = (StackGresClusterPodsScheduling) obj;
    return Objects.equals(backup, other.backup) && Objects.equals(nodeAffinity, other.nodeAffinity)
        && Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(podAffinity, other.podAffinity)
        && Objects.equals(podAntiAffinity, other.podAntiAffinity)
        && Objects.equals(preemptionPolicy, other.preemptionPolicy)
        && Objects.equals(priorityClassName, other.priorityClassName)
        && Objects.equals(runtimeClassName, other.runtimeClassName)
        && Objects.equals(schedulerName, other.schedulerName)
        && Objects.equals(tolerations, other.tolerations)
        && Objects.equals(topologySpreadConstraints, other.topologySpreadConstraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(backup, nodeAffinity, nodeSelector, podAffinity, podAntiAffinity,
        preemptionPolicy, priorityClassName, runtimeClassName, schedulerName, tolerations,
        topologySpreadConstraints);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
