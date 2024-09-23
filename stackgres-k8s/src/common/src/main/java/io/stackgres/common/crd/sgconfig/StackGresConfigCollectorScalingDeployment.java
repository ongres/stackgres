/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Affinity;
import io.stackgres.common.crd.ResourceRequirements;
import io.stackgres.common.crd.Toleration;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigCollectorScalingDeployment {

  private List<StackGresConfigCollectorScalingDeploymentSgCluster> sgClusters;

  private Map<String, String> annotations;

  private ResourceRequirements resources;

  private Map<String, String> nodeSelector;

  private List<Toleration> tolerations;

  private Affinity affinity;

  public List<StackGresConfigCollectorScalingDeploymentSgCluster> getSgClusters() {
    return sgClusters;
  }

  public void setSgClusters(List<StackGresConfigCollectorScalingDeploymentSgCluster> sgClusters) {
    this.sgClusters = sgClusters;
  }

  public Map<String, String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
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

  public Affinity getAffinity() {
    return affinity;
  }

  public void setAffinity(Affinity affinity) {
    this.affinity = affinity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(affinity, annotations, nodeSelector, resources, sgClusters, tolerations);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCollectorScalingDeployment)) {
      return false;
    }
    StackGresConfigCollectorScalingDeployment other = (StackGresConfigCollectorScalingDeployment) obj;
    return Objects.equals(affinity, other.affinity)
        && Objects.equals(annotations, other.annotations)
        && Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(resources, other.resources)
        && Objects.equals(sgClusters, other.sgClusters)
        && Objects.equals(tolerations, other.tolerations);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
