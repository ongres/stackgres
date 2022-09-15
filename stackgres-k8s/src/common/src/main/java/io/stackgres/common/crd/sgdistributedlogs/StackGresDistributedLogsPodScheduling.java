/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDistributedLogsPodScheduling {

  @JsonProperty("nodeSelector")
  private Map<String, String> nodeSelector;

  @JsonProperty("tolerations")
  @Valid
  private List<Toleration> tolerations;

  @JsonProperty("nodeAffinity")
  @Valid
  private NodeAffinity nodeAffinity;

  @ReferencedField("nodeSelector")
  interface NodeSelector extends FieldReference {
  }

  @ReferencedField("nodeAffinity")
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsPodScheduling)) {
      return false;
    }
    StackGresDistributedLogsPodScheduling other = (StackGresDistributedLogsPodScheduling) obj;
    return Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(tolerations, other.tolerations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeSelector, tolerations);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nodeSelector", nodeSelector)
        .add("tolerations", tolerations)
        .toString();
  }
}
