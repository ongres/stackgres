/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigAdminuiService {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
