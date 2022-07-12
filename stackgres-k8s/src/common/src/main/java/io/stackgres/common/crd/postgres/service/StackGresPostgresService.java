/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresPostgresService {

  private Boolean enabled;

  @ValidEnum(enumClass = StackGresPostgresServiceType.class, allowNulls = true,
      message = "type must be one of ClusterIP, LoadBalancer, NodePort or ExternalName")
  private String type;

  private List<String> externalIPs;

  private String loadBalancerIP;

  public StackGresPostgresService() {}

  public StackGresPostgresService(Boolean enabled, String type, List<String> externalIPs,
      String loadBalancerIP) {
    this.enabled = enabled;
    this.type = type;
    this.externalIPs = externalIPs;
    this.loadBalancerIP = loadBalancerIP;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getExternalIPs() {
    return externalIPs;
  }

  public void setExternalIPs(List<String> externalIPs) {
    this.externalIPs = externalIPs;
  }

  public String getLoadBalancerIP() {
    return loadBalancerIP;
  }

  public void setLoadBalancerIP(String loadBalancerIP) {
    this.loadBalancerIP = loadBalancerIP;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, type, externalIPs, loadBalancerIP);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPostgresService)) {
      return false;
    }
    StackGresPostgresService other = (StackGresPostgresService) obj;
    return Objects.equals(enabled, other.enabled)
        && Objects.equals(type, other.type)
        && Objects.equals(externalIPs, other.externalIPs)
        && Objects.equals(loadBalancerIP, other.loadBalancerIP);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
