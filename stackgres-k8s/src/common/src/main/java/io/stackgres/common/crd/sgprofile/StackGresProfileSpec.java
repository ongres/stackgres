/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.util.Objects;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresProfileSpec implements KubernetesResource {

  private static final long serialVersionUID = -1037668102382589521L;

  @JsonProperty("cpu")
  @NotBlank(message = "cpu must be provided")
  private String cpu;

  @JsonProperty("memory")
  @NotBlank(message = "memory must be provided")
  private String memory;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresProfileSpec that = (StackGresProfileSpec) o;
    return Objects.equals(cpu, that.cpu)
        && Objects.equals(memory, that.memory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cpu, memory);
  }
}
