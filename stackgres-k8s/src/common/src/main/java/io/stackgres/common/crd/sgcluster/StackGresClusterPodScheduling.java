/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterPodScheduling {

  @JsonProperty("nodeSelector")
  private Map<String, String> nodeSelector;

  @JsonProperty("nodeAffinity")
  @Valid
  private NodeAffinity nodeAffinity;

  @JsonProperty("tolerations")
  @Valid
  private List<Toleration> tolerations;

  @JsonProperty("backup")
  @Valid
  private StackGresClusterPodSchedulingBackup backup;

  @ReferencedField("nodeSelector")
  interface NodeSelector extends FieldReference {
  }

  @ReferencedField("nodeSelector")
  interface NodeAffinityField extends FieldReference {
  }

  @ReferencedField("tolerations")
  interface TolerationField extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "nodeSelector can not be empty.",
      payload = NodeSelector.class)
  public boolean isNodeSelectorNotEmpty() {
    return nodeSelector == null || !nodeSelector.isEmpty();
  }

  public Map<String, String> getNodeSelector() {
    return nodeSelector;
  }

  public void setNodeSelector(Map<String, String> nodeSelector) {
    this.nodeSelector = nodeSelector;
  }

  public NodeAffinity getNodeAffinity() {
    return nodeAffinity;
  }

  public void setNodeAffinity(NodeAffinity nodeAffinity) {
    this.nodeAffinity = nodeAffinity;
  }

  public List<Toleration> getTolerations() {
    return tolerations;
  }

  public void setTolerations(List<Toleration> tolerations) {
    this.tolerations = tolerations;
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
    return Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(tolerations, other.tolerations)
        && Objects.equals(backup, other.backup)
        && Objects.equals(nodeAffinity, other.nodeAffinity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeSelector, tolerations, nodeAffinity, backup);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
