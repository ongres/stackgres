/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

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
import io.stackgres.common.crd.Toleration;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsSpecScheduling {

  @JsonProperty("tolerations")
  private List<Toleration> tolerations;

  @JsonProperty("nodeSelector")
  private Map<String, String> nodeSelector;

  @JsonProperty("nodeAffinity")
  @Valid
  private NodeAffinity nodeAffinity;

  public List<Toleration> getTolerations() {
    return tolerations;
  }

  public void setTolerations(List<Toleration> tolerations) {
    this.tolerations = tolerations;
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

  @Override
  public int hashCode() {
    return Objects.hash(tolerations);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsSpecScheduling)) {
      return false;
    }
    StackGresDbOpsSpecScheduling other = (StackGresDbOpsSpecScheduling) obj;
    return Objects.equals(tolerations, other.tolerations)
        && Objects.equals(nodeAffinity, other.nodeAffinity)
        && Objects.equals(nodeSelector, other.nodeSelector);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
