/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterPodScheduling {

  private Map<String, String> nodeSelector;

  @Valid
  private List<Toleration> tolerations;

  @Valid
  private NodeAffinity nodeAffinity;

  private String priorityClassName;

  @Valid
  private PodAffinity podAffinity;

  @Valid
  private PodAntiAffinity podAntiAffinity;

  @Valid
  private List<TopologySpreadConstraint> topologySpreadConstraints;

  @Valid
  private StackGresClusterPodSchedulingBackup backup;

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

  public String getPriorityClassName() {
    return priorityClassName;
  }

  public void setPriorityClassName(String priorityClassName) {
    this.priorityClassName = priorityClassName;
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

  public StackGresClusterPodSchedulingBackup getBackup() {
    return backup;
  }

  public void setBackup(StackGresClusterPodSchedulingBackup backup) {
    this.backup = backup;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPodScheduling)) {
      return false;
    }
    StackGresClusterPodScheduling other = (StackGresClusterPodScheduling) obj;
    return Objects.equals(backup, other.backup) && Objects.equals(nodeAffinity, other.nodeAffinity)
        && Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(podAffinity, other.podAffinity)
        && Objects.equals(podAntiAffinity, other.podAntiAffinity)
        && Objects.equals(tolerations, other.tolerations)
        && Objects.equals(topologySpreadConstraints, other.topologySpreadConstraints)
        && Objects.equals(priorityClassName, other.priorityClassName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(backup, nodeAffinity, nodeSelector, podAffinity, podAntiAffinity,
        tolerations, topologySpreadConstraints, priorityClassName);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
