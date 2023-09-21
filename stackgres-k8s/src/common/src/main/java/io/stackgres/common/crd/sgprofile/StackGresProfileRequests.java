/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresProfileRequests {

  private String cpu;

  private String memory;

  @Valid
  private Map<String, StackGresProfileContainer> containers;

  @Valid
  private Map<String, StackGresProfileContainer> initContainers;

  public String getCpu() {
    return cpu;
  }

  public void setCpu(String cpu) {
    this.cpu = cpu;
  }

  public String getMemory() {
    return memory;
  }

  public void setMemory(String memory) {
    this.memory = memory;
  }

  public Map<String, StackGresProfileContainer> getContainers() {
    return containers;
  }

  public void setContainers(Map<String, StackGresProfileContainer> containers) {
    this.containers = containers;
  }

  public Map<String, StackGresProfileContainer> getInitContainers() {
    return initContainers;
  }

  public void setInitContainers(Map<String, StackGresProfileContainer> initContainers) {
    this.initContainers = initContainers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(containers, cpu, initContainers, memory);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresProfileRequests)) {
      return false;
    }
    StackGresProfileRequests other = (StackGresProfileRequests) obj;
    return Objects.equals(containers, other.containers) && Objects.equals(cpu, other.cpu)
        && Objects.equals(initContainers, other.initContainers)
        && Objects.equals(memory, other.memory);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
