/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.SessionAffinityConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomServicePort;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceNodePort;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ServiceSpec.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ServicePort.class),
        @BuildableReference(CustomServicePort.class)
    })
public class StackGresClusterPostgresService extends StackGresPostgresService {

  private static final long serialVersionUID = 1L;

  @Valid
  private List<CustomServicePort> customPorts;

  public StackGresClusterPostgresService() {
    super();
  }

  public StackGresClusterPostgresService(Boolean allocateLoadBalancerNodePorts, String clusterIP,
      List<String> clusterIPs, List<String> externalIPs, String externalName,
      String externalTrafficPolicy, Integer healthCheckNodePort, String internalTrafficPolicy,
      List<String> ipFamilies, String ipFamilyPolicy, String loadBalancerClass,
      String loadBalancerIP, List<String> loadBalancerSourceRanges, List<ServicePort> ports,
      Boolean publishNotReadyAddresses, Map<String, String> selector, String sessionAffinity,
      SessionAffinityConfig sessionAffinityConfig, String trafficDistribution, String type,
      StackGresPostgresServiceNodePort nodePorts) {
    super(allocateLoadBalancerNodePorts, clusterIP, clusterIPs, externalIPs, externalName,
        externalTrafficPolicy, healthCheckNodePort, internalTrafficPolicy, ipFamilies,
        ipFamilyPolicy, loadBalancerClass, loadBalancerIP, loadBalancerSourceRanges, ports,
        publishNotReadyAddresses, selector, sessionAffinity, sessionAffinityConfig,
        trafficDistribution, type, nodePorts);
  }

  public StackGresClusterPostgresService(StackGresPostgresService service) {
    super(service.getAllocateLoadBalancerNodePorts(), service.getClusterIP(),
        service.getClusterIPs(), service.getExternalIPs(), service.getExternalName(),
        service.getExternalTrafficPolicy(), service.getHealthCheckNodePort(),
        service.getInternalTrafficPolicy(), service.getIpFamilies(), service.getIpFamilyPolicy(),
        service.getLoadBalancerClass(), service.getLoadBalancerIP(),
        service.getLoadBalancerSourceRanges(), service.getPorts(),
        service.getPublishNotReadyAddresses(), service.getSelector(), service.getSessionAffinity(),
        service.getSessionAffinityConfig(), service.getTrafficDistribution(), service.getType(),
        service.getNodePorts());
  }

  public List<CustomServicePort> getCustomPorts() {
    return customPorts;
  }

  public void setCustomPorts(List<CustomServicePort> customPorts) {
    this.customPorts = customPorts;
  }

  @Override
  public Boolean getEnabled() {
    return super.getEnabled();
  }

  @Override
  public void setEnabled(Boolean enabled) {
    super.setEnabled(enabled);
  }

  @Override
  public String getType() {
    return super.getType();
  }

  @Override
  public void setType(String type) {
    super.setType(type);
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(customPorts);
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
    if (!(obj instanceof StackGresClusterPostgresService)) {
      return false;
    }
    StackGresClusterPostgresService other = (StackGresClusterPostgresService) obj;
    return Objects.equals(customPorts, other.customPorts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
