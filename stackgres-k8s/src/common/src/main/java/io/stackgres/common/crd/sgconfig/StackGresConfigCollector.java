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
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresConfigCollector {

  private String name;

  private StackGresConfigImage image;

  private StackGresConfigCollectorDaemonset daemonset;

  private Map<String, String> annotations;

  private ResourceRequirements resources;

  private Map<String, String> nodeSelector;

  private List<Toleration> tolerations;

  private Affinity affinity;

  private StackGresConfigServiceAccount serviceAccount;

  private StackGresConfigCollectorService service;

  private List<ContainerPort> ports;

  private List<VolumeMount> volumeMounts;

  private List<Volume> volumes;

  private StackGresConfigCollectorConfig config;

  private StackGresConfigCollectorPrometheusOperator prometheusOperator;

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

  public StackGresConfigCollectorDaemonset getDaemonset() {
    return daemonset;
  }

  public void setDaemonset(StackGresConfigCollectorDaemonset daemonset) {
    this.daemonset = daemonset;
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

  public StackGresConfigServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(StackGresConfigServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public StackGresConfigCollectorService getService() {
    return service;
  }

  public void setService(StackGresConfigCollectorService service) {
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

  public StackGresConfigCollectorConfig getConfig() {
    return config;
  }

  public void setConfig(StackGresConfigCollectorConfig config) {
    this.config = config;
  }

  public StackGresConfigCollectorPrometheusOperator getPrometheusOperator() {
    return prometheusOperator;
  }

  public void setPrometheusOperator(StackGresConfigCollectorPrometheusOperator prometheusOperator) {
    this.prometheusOperator = prometheusOperator;
  }

  @Override
  public int hashCode() {
    return Objects.hash(affinity, annotations, config, daemonset, image, name, nodeSelector, ports,
        prometheusOperator, resources, service, serviceAccount, tolerations, volumeMounts, volumes);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCollector)) {
      return false;
    }
    StackGresConfigCollector other = (StackGresConfigCollector) obj;
    return Objects.equals(affinity, other.affinity)
        && Objects.equals(annotations, other.annotations) && Objects.equals(config, other.config)
        && Objects.equals(daemonset, other.daemonset) && Objects.equals(image, other.image)
        && Objects.equals(name, other.name) && Objects.equals(nodeSelector, other.nodeSelector)
        && Objects.equals(ports, other.ports)
        && Objects.equals(prometheusOperator, other.prometheusOperator)
        && Objects.equals(resources, other.resources) && Objects.equals(service, other.service)
        && Objects.equals(serviceAccount, other.serviceAccount)
        && Objects.equals(tolerations, other.tolerations)
        && Objects.equals(volumeMounts, other.volumeMounts)
        && Objects.equals(volumes, other.volumes);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
