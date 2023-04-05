/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.postgres.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.SessionAffinityConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ServiceSpec;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS",
    justification = "equals and hashCode are unused")
public class PostgresService extends ServiceSpec {

  private static final long serialVersionUID = 1L;

  private Boolean enabled;

  private String type;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
