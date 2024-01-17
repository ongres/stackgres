/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.shardingsphere;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ComputeNodeSpec {

  private ComputeNodeBootstrap bootstrap;

  private List<EnvVar> env;

  private List<LocalObjectReference> imagePullSecrets;

  private List<ComputeNodePortBinding> portBindings;

  private ComputeNodeProbes probes;

  private Integer replicas;

  private ResourceRequirements resources;

  private LabelSelector selector;

  private String serverVersion;

  private String serviceType;

  private ComputeNodeStorageNodeConnector storageNodeConnector;

  public ComputeNodeBootstrap getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(ComputeNodeBootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  public List<EnvVar> getEnv() {
    return env;
  }

  public void setEnv(List<EnvVar> env) {
    this.env = env;
  }

  public List<LocalObjectReference> getImagePullSecrets() {
    return imagePullSecrets;
  }

  public void setImagePullSecrets(List<LocalObjectReference> imagePullSecrets) {
    this.imagePullSecrets = imagePullSecrets;
  }

  public List<ComputeNodePortBinding> getPortBindings() {
    return portBindings;
  }

  public void setPortBindings(List<ComputeNodePortBinding> portBindings) {
    this.portBindings = portBindings;
  }

  public ComputeNodeProbes getProbes() {
    return probes;
  }

  public void setProbes(ComputeNodeProbes probes) {
    this.probes = probes;
  }

  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }

  public LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(LabelSelector selector) {
    this.selector = selector;
  }

  public String getServerVersion() {
    return serverVersion;
  }

  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public ComputeNodeStorageNodeConnector getStorageNodeConnector() {
    return storageNodeConnector;
  }

  public void setStorageNodeConnector(ComputeNodeStorageNodeConnector storageNodeConnector) {
    this.storageNodeConnector = storageNodeConnector;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bootstrap, env, imagePullSecrets, portBindings, probes, replicas, resources, selector,
        serverVersion, serviceType, storageNodeConnector);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComputeNodeSpec)) {
      return false;
    }
    ComputeNodeSpec other = (ComputeNodeSpec) obj;
    return Objects.equals(bootstrap, other.bootstrap) && Objects.equals(env, other.env)
        && Objects.equals(imagePullSecrets, other.imagePullSecrets) && Objects.equals(portBindings, other.portBindings)
        && Objects.equals(probes, other.probes) && Objects.equals(replicas, other.replicas)
        && Objects.equals(resources, other.resources) && Objects.equals(selector, other.selector)
        && Objects.equals(serverVersion, other.serverVersion) && Objects.equals(serviceType, other.serviceType)
        && Objects.equals(storageNodeConnector, other.storageNodeConnector);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
