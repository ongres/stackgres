/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.SessionAffinityConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ServiceSpec;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ServiceSpec.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ServicePort.class),
    })
public class StackGresPostgresService extends ServiceSpec {

  private static final long serialVersionUID = 1L;

  protected Boolean enabled;

  @ValidEnum(enumClass = StackGresPostgresServiceType.class, allowNulls = true,
      message = "type must be one of ClusterIP, LoadBalancer or NodePort")
  protected String type;

  protected StackGresPostgresServiceNodePort nodePorts;

  public StackGresPostgresService() {
    super();
  }

  public StackGresPostgresService(Boolean allocateLoadBalancerNodePorts, String clusterIP,
      List<String> clusterIPs, List<String> externalIPs, String externalName,
      String externalTrafficPolicy, Integer healthCheckNodePort, String internalTrafficPolicy,
      List<String> ipFamilies, String ipFamilyPolicy, String loadBalancerClass,
      String loadBalancerIP, List<String> loadBalancerSourceRanges, List<ServicePort> ports,
      Boolean publishNotReadyAddresses, Map<String, String> selector, String sessionAffinity,
      SessionAffinityConfig sessionAffinityConfig, String type, StackGresPostgresServiceNodePort nodePorts) {
    super(allocateLoadBalancerNodePorts, clusterIP, clusterIPs, externalIPs, externalName,
        externalTrafficPolicy, healthCheckNodePort, internalTrafficPolicy, ipFamilies,
        ipFamilyPolicy, loadBalancerClass, loadBalancerIP, loadBalancerSourceRanges, ports,
        publishNotReadyAddresses, selector, sessionAffinity, sessionAffinityConfig, null);
    this.type = type;
    this.nodePorts = nodePorts;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public Boolean getAllocateLoadBalancerNodePorts() {
    return super.getAllocateLoadBalancerNodePorts();
  }

  @Override
  public void setAllocateLoadBalancerNodePorts(Boolean allocateLoadBalancerNodePorts) {
    super.setAllocateLoadBalancerNodePorts(allocateLoadBalancerNodePorts);
  }

  @Override
  public String getClusterIP() {
    return super.getClusterIP();
  }

  @Override
  public void setClusterIP(String clusterIP) {
    super.setClusterIP(clusterIP);
  }

  @Override
  public List<String> getClusterIPs() {
    return super.getClusterIPs();
  }

  @Override
  public void setClusterIPs(List<String> clusterIPs) {
    super.setClusterIPs(clusterIPs);
  }

  @Override
  public List<String> getExternalIPs() {
    return super.getExternalIPs();
  }

  @Override
  public void setExternalIPs(List<String> externalIPs) {
    super.setExternalIPs(externalIPs);
  }

  @Override
  public String getExternalName() {
    return super.getExternalName();
  }

  @Override
  public void setExternalName(String externalName) {
    super.setExternalName(externalName);
  }

  @Override
  public String getExternalTrafficPolicy() {
    return super.getExternalTrafficPolicy();
  }

  @Override
  public void setExternalTrafficPolicy(String externalTrafficPolicy) {
    super.setExternalTrafficPolicy(externalTrafficPolicy);
  }

  @Override
  public Integer getHealthCheckNodePort() {
    return super.getHealthCheckNodePort();
  }

  @Override
  public void setHealthCheckNodePort(Integer healthCheckNodePort) {
    super.setHealthCheckNodePort(healthCheckNodePort);
  }

  @Override
  public String getInternalTrafficPolicy() {
    return super.getInternalTrafficPolicy();
  }

  @Override
  public void setInternalTrafficPolicy(String internalTrafficPolicy) {
    super.setInternalTrafficPolicy(internalTrafficPolicy);
  }

  @Override
  public List<String> getIpFamilies() {
    return super.getIpFamilies();
  }

  @Override
  public void setIpFamilies(List<String> ipFamilies) {
    super.setIpFamilies(ipFamilies);
  }

  @Override
  public String getIpFamilyPolicy() {
    return super.getIpFamilyPolicy();
  }

  @Override
  public void setIpFamilyPolicy(String ipFamilyPolicy) {
    super.setIpFamilyPolicy(ipFamilyPolicy);
  }

  @Override
  public String getLoadBalancerClass() {
    return super.getLoadBalancerClass();
  }

  @Override
  public void setLoadBalancerClass(String loadBalancerClass) {
    super.setLoadBalancerClass(loadBalancerClass);
  }

  @Override
  public String getLoadBalancerIP() {
    return super.getLoadBalancerIP();
  }

  @Override
  public void setLoadBalancerIP(String loadBalancerIP) {
    super.setLoadBalancerIP(loadBalancerIP);
  }

  @Override
  public List<String> getLoadBalancerSourceRanges() {
    return super.getLoadBalancerSourceRanges();
  }

  @Override
  public void setLoadBalancerSourceRanges(List<String> loadBalancerSourceRanges) {
    super.setLoadBalancerSourceRanges(loadBalancerSourceRanges);
  }

  @Override
  public List<ServicePort> getPorts() {
    return super.getPorts();
  }

  @Override
  public void setPorts(List<ServicePort> ports) {
    super.setPorts(ports);
  }

  @Override
  public Boolean getPublishNotReadyAddresses() {
    return super.getPublishNotReadyAddresses();
  }

  @Override
  public void setPublishNotReadyAddresses(Boolean publishNotReadyAddresses) {
    super.setPublishNotReadyAddresses(publishNotReadyAddresses);
  }

  @Override
  public Map<String, String> getSelector() {
    return super.getSelector();
  }

  @Override
  public void setSelector(Map<String, String> selector) {
    super.setSelector(selector);
  }

  @Override
  public String getSessionAffinity() {
    return super.getSessionAffinity();
  }

  @Override
  public void setSessionAffinity(String sessionAffinity) {
    super.setSessionAffinity(sessionAffinity);
  }

  @Override
  public SessionAffinityConfig getSessionAffinityConfig() {
    return super.getSessionAffinityConfig();
  }

  @Override
  public void setSessionAffinityConfig(SessionAffinityConfig sessionAffinityConfig) {
    super.setSessionAffinityConfig(sessionAffinityConfig);
  }

  public StackGresPostgresServiceNodePort getNodePorts() {
    return nodePorts;
  }

  public void setNodePorts(StackGresPostgresServiceNodePort nodePorts) {
    this.nodePorts = nodePorts;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(enabled, type, nodePorts);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresPostgresService other)) {
      return false;
    }
    return Objects.equals(enabled, other.enabled)
      && Objects.equals(type, other.type)
      && Objects.equals(nodePorts, other.nodePorts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
