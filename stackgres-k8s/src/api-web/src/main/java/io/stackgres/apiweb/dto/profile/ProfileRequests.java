/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ProfileRequests {

  @JsonProperty("cpu")
  private String cpu;

  @JsonProperty("memory")
  private String memory;

  @JsonProperty("containers")
  private Map<String, ProfileContainer> containers;

  @JsonProperty("initContainers")
  private Map<String, ProfileContainer> initContainers;

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

  public Map<String, ProfileContainer> getContainers() {
    return containers;
  }

  public void setContainers(Map<String, ProfileContainer> containers) {
    this.containers = containers;
  }

  public Map<String, ProfileContainer> getInitContainers() {
    return initContainers;
  }

  public void setInitContainers(Map<String, ProfileContainer> initContainers) {
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
    if (!(obj instanceof ProfileRequests)) {
      return false;
    }
    ProfileRequests other = (ProfileRequests) obj;
    return Objects.equals(containers, other.containers) && Objects.equals(cpu, other.cpu)
        && Objects.equals(initContainers, other.initContainers)
        && Objects.equals(memory, other.memory);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
