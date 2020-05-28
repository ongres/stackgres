/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class KubernetesPod {

  @JsonProperty("namespace")
  @NotNull
  private String namespace;

  @JsonProperty("name")
  @NotNull
  private String name;

  @JsonProperty("role")
  private String role;

  @JsonProperty("ip")
  private String ip;

  @JsonProperty("status")
  @NotNull
  private String status;

  @JsonProperty("containers")
  private Integer containers;

  @JsonProperty("containersReady")
  private Integer containersReady;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getContainers() {
    return containers;
  }

  public void setContainers(Integer containers) {
    this.containers = containers;
  }

  public Integer getContainersReady() {
    return containersReady;
  }

  public void setContainersReady(Integer containersReady) {
    this.containersReady = containersReady;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("namespace", namespace)
        .add("name", name)
        .add("role", role)
        .add("ip", ip)
        .add("status", status)
        .add("containers", containers)
        .add("containersReady", containersReady)
        .toString();
  }

}
