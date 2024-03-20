/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.shardingsphere;

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
public class ComputeNodePortBinding {

  private Integer containerPort;

  @JsonProperty("hostIP")
  private String hostIp;

  private String name;

  private Integer nodePort;

  private String protocol;

  private Integer servicePort;

  public Integer getContainerPort() {
    return containerPort;
  }

  public void setContainerPort(Integer containerPort) {
    this.containerPort = containerPort;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getNodePort() {
    return nodePort;
  }

  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public Integer getServicePort() {
    return servicePort;
  }

  public void setServicePort(Integer servicePort) {
    this.servicePort = servicePort;
  }

  @Override
  public int hashCode() {
    return Objects.hash(containerPort, hostIp, name, nodePort, protocol, servicePort);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ComputeNodePortBinding)) {
      return false;
    }
    ComputeNodePortBinding other = (ComputeNodePortBinding) obj;
    return Objects.equals(containerPort, other.containerPort) && Objects.equals(hostIp, other.hostIp)
        && Objects.equals(name, other.name) && Objects.equals(nodePort, other.nodePort)
        && Objects.equals(protocol, other.protocol) && Objects.equals(servicePort, other.servicePort);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
