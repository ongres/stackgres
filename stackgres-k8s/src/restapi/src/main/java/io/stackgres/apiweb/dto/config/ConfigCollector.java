/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Affinity;
import io.stackgres.common.crd.ResourceRequirements;
import io.stackgres.common.crd.Toleration;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigCollector {

  private String name;

  private ConfigImage image;

  private ConfigCollectorReceiver receivers;

  private Map<String, String> annotations;

  private ResourceRequirements resources;

  private Map<String, String> nodeSelector;

  private List<Toleration> tolerations;

  private Affinity affinity;

  private ConfigServiceAccount serviceAccount;

  private ConfigCollectorService service;

  private List<ContainerPort> ports;

  private List<VolumeMount> volumeMounts;

  private List<Volume> volumes;

  private Map<String, Object> config;

  private ConfigCollectorPrometheusOperator prometheusOperator;

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

  public ConfigCollectorReceiver getReceivers() {
    return receivers;
  }

  public void setReceivers(ConfigCollectorReceiver receivers) {
    this.receivers = receivers;
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

  public ConfigServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ConfigServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public ConfigCollectorService getService() {
    return service;
  }

  public void setService(ConfigCollectorService service) {
    this.service = service;
  }

  public List<ContainerPort> getPorts() {
    return ports;
  }

  public void setPorts(List<ContainerPort> ports) {
    this.ports = ports;
  }

  public List<VolumeMount> getVolumeMounts() {
    return volumeMounts;
  }

  public void setVolumeMounts(List<VolumeMount> volumeMounts) {
    this.volumeMounts = volumeMounts;
  }

  public List<Volume> getVolumes() {
    return volumes;
  }

  public void setVolumes(List<Volume> volumes) {
    this.volumes = volumes;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public ConfigCollectorPrometheusOperator getPrometheusOperator() {
    return prometheusOperator;
  }

  public void setPrometheusOperator(ConfigCollectorPrometheusOperator prometheusOperator) {
    this.prometheusOperator = prometheusOperator;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
