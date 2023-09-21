/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ProfileRequests {

  private String cpu;

  private String memory;

  private Map<String, ProfileContainer> containers;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
