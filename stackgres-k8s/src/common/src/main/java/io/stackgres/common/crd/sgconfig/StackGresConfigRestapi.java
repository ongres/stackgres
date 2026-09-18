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
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Affinity;
import io.stackgres.common.crd.ResourceRequirements;
import io.stackgres.common.crd.Toleration;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigRestapi extends AdditionalProperties {

  private String name;

  private StackGresConfigImage image;

  private Map<String, String> annotations;

  private ResourceRequirements resources;

  private String schedulerName;

  private String runtimeClassName;

  private String preemptionPolicy;

  private String priorityClassName;

  private Map<String, String> nodeSelector;

  private List<Toleration> tolerations;

  private Affinity affinity;

  private StackGresConfigServiceAccount serviceAccount;

  private StackGresConfigService service;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StackGresConfigImage getImage() {
    return image;
  }

  public void setImage(StackGresConfigImage image) {
    this.image = image;
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

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }

  public String getRuntimeClassName() {
    return runtimeClassName;
  }

  public void setRuntimeClassName(String runtimeClassName) {
    this.runtimeClassName = runtimeClassName;
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

  public StackGresConfigServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(StackGresConfigServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public StackGresConfigService getService() {
    return service;
  }

  public void setService(StackGresConfigService service) {
    this.service = service;
  }

  @Override
  public int hashCode() {
    return Objects.hash(affinity, annotations, image, name, nodeSelector, preemptionPolicy,
        priorityClassName, resources, runtimeClassName, schedulerName, service, serviceAccount,
        tolerations);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigRestapi)) {
      return false;
    }
    StackGresConfigRestapi other = (StackGresConfigRestapi) obj;
    return Objects.equals(affinity, other.affinity)
        && Objects.equals(annotations, other.annotations) && Objects.equals(image, other.image)
        && Objects.equals(name, other.name) && Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(preemptionPolicy, other.preemptionPolicy)
        && Objects.equals(priorityClassName, other.priorityClassName)
        && Objects.equals(resources, other.resources)
        && Objects.equals(runtimeClassName, other.runtimeClassName)
        && Objects.equals(schedulerName, other.schedulerName)
        && Objects.equals(service, other.service)
        && Objects.equals(serviceAccount, other.serviceAccount)
        && Objects.equals(tolerations, other.tolerations);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
