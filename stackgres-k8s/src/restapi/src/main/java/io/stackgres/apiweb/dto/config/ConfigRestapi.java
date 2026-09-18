/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Affinity;
import io.stackgres.common.crd.ResourceRequirements;
import io.stackgres.common.crd.Toleration;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigRestapi extends AdditionalProperties {

  private String name;

  private ConfigImage image;

  private Map<String, String> annotations;

  private ResourceRequirements resources;

  private String schedulerName;

  private String runtimeClassName;

  private String preemptionPolicy;

  private String priorityClassName;

  private Map<String, String> nodeSelector;

  private List<Toleration> tolerations;

  private Affinity affinity;

  private ConfigServiceAccount serviceAccount;

  private ConfigService service;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ConfigImage getImage() {
    return image;
  }

  public void setImage(ConfigImage image) {
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

  public ConfigServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ConfigServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public ConfigService getService() {
    return service;
  }

  public void setService(ConfigService service) {
    this.service = service;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
