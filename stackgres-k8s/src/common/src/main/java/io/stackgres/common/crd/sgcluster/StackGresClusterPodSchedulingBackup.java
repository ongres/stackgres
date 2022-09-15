/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.NodeAffinity;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterPodSchedulingBackup {

  @JsonProperty("nodeSelector")
  private Map<String, String> nodeSelector;

  @JsonProperty("nodeAffinity")
  @Valid
  private NodeAffinity nodeAffinity;

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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPodSchedulingBackup)) {
      return false;
    }
    StackGresClusterPodSchedulingBackup other = (StackGresClusterPodSchedulingBackup) obj;
    return Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(nodeAffinity, other.nodeAffinity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeSelector, nodeAffinity);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
