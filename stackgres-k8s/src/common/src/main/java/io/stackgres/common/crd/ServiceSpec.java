/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.SessionAffinityConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"clusterIP", "clusterIPs", "externalName",
        "ports", "publishNotReadyAddresses", "selector"})
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ServiceSpec.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ServicePort.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.SessionAffinityConfig.class),
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class ServiceSpec extends io.fabric8.kubernetes.api.model.ServiceSpec {

  private static final long serialVersionUID = 1L;

  public ServiceSpec() {
    super();
  }

  public ServiceSpec(Boolean allocateLoadBalancerNodePorts, String clusterIP,
      List<String> clusterIPs, List<String> externalIPs, String externalName,
      String externalTrafficPolicy, Integer healthCheckNodePort, String internalTrafficPolicy,
      List<String> ipFamilies, String ipFamilyPolicy, String loadBalancerClass,
      String loadBalancerIP, List<String> loadBalancerSourceRanges, List<ServicePort> ports,
      Boolean publishNotReadyAddresses, Map<String, String> selector, String sessionAffinity,
      SessionAffinityConfig sessionAffinityConfig, String trafficDistribution, String type) {
    super(allocateLoadBalancerNodePorts, clusterIP, clusterIPs, externalIPs, externalName,
        externalTrafficPolicy, healthCheckNodePort, internalTrafficPolicy, ipFamilies,
        ipFamilyPolicy, loadBalancerClass, loadBalancerIP, loadBalancerSourceRanges, ports,
        publishNotReadyAddresses, selector, sessionAffinity, sessionAffinityConfig,
        trafficDistribution, type);
  }

  @Override
  public Boolean getAllocateLoadBalancerNodePorts() {
    return super.getAllocateLoadBalancerNodePorts();
  }

  @Override
  public String getClusterIP() {
    return super.getClusterIP();
  }

  @Override
  public List<String> getClusterIPs() {
    return super.getClusterIPs();
  }

  @Override
  public List<String> getExternalIPs() {
    return super.getExternalIPs();
  }

  @Override
  public String getExternalName() {
    return super.getExternalName();
  }

  @Override
  public String getExternalTrafficPolicy() {
    return super.getExternalTrafficPolicy();
  }

  @Override
  public Integer getHealthCheckNodePort() {
    return super.getHealthCheckNodePort();
  }

  @Override
  public String getInternalTrafficPolicy() {
    return super.getInternalTrafficPolicy();
  }

  @Override
  public List<String> getIpFamilies() {
    return super.getIpFamilies();
  }

  @Override
  public String getIpFamilyPolicy() {
    return super.getIpFamilyPolicy();
  }

  @Override
  public String getLoadBalancerClass() {
    return super.getLoadBalancerClass();
  }

  @Override
  public String getLoadBalancerIP() {
    return super.getLoadBalancerIP();
  }

  @Override
  public List<String> getLoadBalancerSourceRanges() {
    return super.getLoadBalancerSourceRanges();
  }

  @Override
  public List<ServicePort> getPorts() {
    return super.getPorts();
  }

  @Override
  public Boolean getPublishNotReadyAddresses() {
    return super.getPublishNotReadyAddresses();
  }

  @Override
  public Map<String, String> getSelector() {
    return super.getSelector();
  }

  @Override
  public String getSessionAffinity() {
    return super.getSessionAffinity();
  }

  @Override
  public SessionAffinityConfig getSessionAffinityConfig() {
    return super.getSessionAffinityConfig();
  }

  @Override
  public String getTrafficDistribution() {
    return super.getTrafficDistribution();
  }

  @Override
  public String getType() {
    return super.getType();
  }

  @Override
  public Map<String, Object> getAdditionalProperties() {
    return super.getAdditionalProperties();
  }

  @Override
  public void setAllocateLoadBalancerNodePorts(Boolean allocateLoadBalancerNodePorts) {
    super.setAllocateLoadBalancerNodePorts(allocateLoadBalancerNodePorts);
  }

  @Override
  public void setClusterIP(String clusterIP) {
    super.setClusterIP(clusterIP);
  }

  @Override
  public void setClusterIPs(List<String> clusterIPs) {
    super.setClusterIPs(clusterIPs);
  }

  @Override
  public void setExternalIPs(List<String> externalIPs) {
    super.setExternalIPs(externalIPs);
  }

  @Override
  public void setExternalName(String externalName) {
    super.setExternalName(externalName);
  }

  @Override
  public void setExternalTrafficPolicy(String externalTrafficPolicy) {
    super.setExternalTrafficPolicy(externalTrafficPolicy);
  }

  @Override
  public void setHealthCheckNodePort(Integer healthCheckNodePort) {
    super.setHealthCheckNodePort(healthCheckNodePort);
  }

  @Override
  public void setInternalTrafficPolicy(String internalTrafficPolicy) {
    super.setInternalTrafficPolicy(internalTrafficPolicy);
  }

  @Override
  public void setIpFamilies(List<String> ipFamilies) {
    super.setIpFamilies(ipFamilies);
  }

  @Override
  public void setIpFamilyPolicy(String ipFamilyPolicy) {
    super.setIpFamilyPolicy(ipFamilyPolicy);
  }

  @Override
  public void setLoadBalancerClass(String loadBalancerClass) {
    super.setLoadBalancerClass(loadBalancerClass);
  }

  @Override
  public void setLoadBalancerIP(String loadBalancerIP) {
    super.setLoadBalancerIP(loadBalancerIP);
  }

  @Override
  public void setLoadBalancerSourceRanges(List<String> loadBalancerSourceRanges) {
    super.setLoadBalancerSourceRanges(loadBalancerSourceRanges);
  }

  @Override
  public void setPorts(List<ServicePort> ports) {
    super.setPorts(ports);
  }

  @Override
  public void setPublishNotReadyAddresses(Boolean publishNotReadyAddresses) {
    super.setPublishNotReadyAddresses(publishNotReadyAddresses);
  }

  @Override
  public void setSelector(Map<String, String> selector) {
    super.setSelector(selector);
  }

  @Override
  public void setSessionAffinity(String sessionAffinity) {
    super.setSessionAffinity(sessionAffinity);
  }

  @Override
  public void setSessionAffinityConfig(SessionAffinityConfig sessionAffinityConfig) {
    super.setSessionAffinityConfig(sessionAffinityConfig);
  }

  @Override
  public void setTrafficDistribution(String trafficDistribution) {
    super.setTrafficDistribution(trafficDistribution);
  }

  @Override
  public void setType(String type) {
    super.setType(type);
  }

  @Override
  public ServiceSpecBuilder toBuilder() {
    return super.toBuilder();
  }

  @Override
  public void setAdditionalProperty(String name, Object value) {
    super.setAdditionalProperty(name, value);
  }

  @Override
  public void setAdditionalProperties(Map<String, Object> additionalProperties) {
    super.setAdditionalProperties(additionalProperties);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof ServiceSpec)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
