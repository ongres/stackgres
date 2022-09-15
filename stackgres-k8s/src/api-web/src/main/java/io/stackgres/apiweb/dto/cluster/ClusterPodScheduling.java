/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.NodeAffinity;
import io.stackgres.common.crd.Toleration;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterPodScheduling {

  private Map<String, String> nodeSelector;

  private NodeAffinity nodeAffinity;

  private List<Toleration> tolerations;

  private ClusterPodSchedulingBackup backup;

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

  public ClusterPodSchedulingBackup getBackup() {
    return backup;
  }

  public void setBackup(ClusterPodSchedulingBackup backup) {
    this.backup = backup;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterPodScheduling that = (ClusterPodScheduling) o;
    return Objects.equals(nodeSelector, that.nodeSelector)
        && Objects.equals(nodeAffinity, that.nodeAffinity)
        && Objects.equals(tolerations, that.tolerations)
        && Objects.equals(backup, that.backup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeSelector, nodeAffinity, tolerations, backup);
  }

}
