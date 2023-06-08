/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.PodAffinity;
import io.stackgres.common.crd.PodAntiAffinity;
import io.stackgres.common.crd.Toleration;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDistributedLogsPodScheduling {

  @JsonProperty("nodeSelector")
  private Map<String, String> nodeSelector;

  @JsonProperty("tolerations")
  @Valid
  private List<Toleration> tolerations;

  @JsonProperty("nodeAffinity")
  @Valid
  private NodeAffinity nodeAffinity;

  @JsonProperty("podAffinity")
  @Valid
  private PodAffinity podAffinity;

  @JsonProperty("priorityClassName")
  private String priorityClassName;

  @JsonProperty("podAntiAffinity")
  @Valid
  private PodAntiAffinity podAntiAffinity;

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

  public PodAffinity getPodAffinity() {
    return podAffinity;
  }

  public void setPodAffinity(PodAffinity podAffinity) {
    this.podAffinity = podAffinity;
  }

  public String getPriorityClassName() {
    return priorityClassName;
  }

  public void setPriorityClassName(String priorityClassName) {
    this.priorityClassName = priorityClassName;
  }

  public PodAntiAffinity getPodAntiAffinity() {
    return podAntiAffinity;
  }

  public void setPodAntiAffinity(PodAntiAffinity podAntiAffinity) {
    this.podAntiAffinity = podAntiAffinity;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsPodScheduling)) {
      return false;
    }
    StackGresDistributedLogsPodScheduling other = (StackGresDistributedLogsPodScheduling) obj;
    return Objects.equals(nodeAffinity, other.nodeAffinity)
        && Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(podAffinity, other.podAffinity)
        && Objects.equals(podAntiAffinity, other.podAntiAffinity)
        && Objects.equals(tolerations, other.tolerations)
        && Objects.equals(priorityClassName, other.priorityClassName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeAffinity, nodeSelector, podAffinity, podAntiAffinity, tolerations,
        priorityClassName);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
