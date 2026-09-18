/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresInstanceProfileRequests extends AdditionalProperties {

  private String cpu;

  private String memory;

  private Map<String, @Valid StackGresInstanceProfileContainer> containers;

  private Map<String, @Valid StackGresInstanceProfileContainer> initContainers;

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

  public Map<String, StackGresInstanceProfileContainer> getContainers() {
    return containers;
  }

  public void setContainers(Map<String, StackGresInstanceProfileContainer> containers) {
    this.containers = containers;
  }

  public Map<String, StackGresInstanceProfileContainer> getInitContainers() {
    return initContainers;
  }

  public void setInitContainers(Map<String, StackGresInstanceProfileContainer> initContainers) {
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
    if (!(obj instanceof StackGresInstanceProfileRequests)) {
      return false;
    }
    StackGresInstanceProfileRequests other = (StackGresInstanceProfileRequests) obj;
    return Objects.equals(containers, other.containers) && Objects.equals(cpu, other.cpu)
        && Objects.equals(initContainers, other.initContainers)
        && Objects.equals(memory, other.memory);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
