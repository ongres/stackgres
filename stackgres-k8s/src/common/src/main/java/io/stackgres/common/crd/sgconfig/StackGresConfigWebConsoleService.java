/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigWebConsoleService {

  @JsonProperty("exposeHTTP")
  private Boolean exposeHttp;

  private String type;

  private String loadBalancerIP;

  private List<String> loadBalancerSourceRanges;

  private Integer nodePort;

  @JsonProperty("nodePortHTTP")
  private Integer nodePortHttp;

  public Boolean getExposeHttp() {
    return exposeHttp;
  }

  public void setExposeHttp(Boolean exposeHttp) {
    this.exposeHttp = exposeHttp;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLoadBalancerIP() {
    return loadBalancerIP;
  }

  public void setLoadBalancerIP(String loadBalancerIP) {
    this.loadBalancerIP = loadBalancerIP;
  }

  public List<String> getLoadBalancerSourceRanges() {
    return loadBalancerSourceRanges;
  }

  public void setLoadBalancerSourceRanges(List<String> loadBalancerSourceRanges) {
    this.loadBalancerSourceRanges = loadBalancerSourceRanges;
  }

  public Integer getNodePort() {
    return nodePort;
  }

  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }

  public Integer getNodePortHttp() {
    return nodePortHttp;
  }

  public void setNodePortHttp(Integer nodePortHttp) {
    this.nodePortHttp = nodePortHttp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(exposeHttp, loadBalancerIP, loadBalancerSourceRanges, nodePort,
        nodePortHttp, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigWebConsoleService)) {
      return false;
    }
    StackGresConfigWebConsoleService other = (StackGresConfigWebConsoleService) obj;
    return Objects.equals(exposeHttp, other.exposeHttp)
        && Objects.equals(loadBalancerIP, other.loadBalancerIP)
        && Objects.equals(loadBalancerSourceRanges, other.loadBalancerSourceRanges)
        && Objects.equals(nodePort, other.nodePort)
        && Objects.equals(nodePortHttp, other.nodePortHttp)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
