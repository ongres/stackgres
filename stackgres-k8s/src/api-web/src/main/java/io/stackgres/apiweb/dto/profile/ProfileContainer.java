/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ProfileContainer {

  @JsonProperty("cpu")
  @NotBlank(message = "cpu must be provided")
  private String cpu;

  @JsonProperty("memory")
  @NotBlank(message = "memory must be provided")
  private String memory;

  @JsonProperty("hugePages")
  @Valid
  private ProfileHugePages hugePages;

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

  public ProfileHugePages getHugePages() {
    return hugePages;
  }

  public void setHugePages(ProfileHugePages hugePages) {
    this.hugePages = hugePages;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cpu, hugePages, memory);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ProfileContainer)) {
      return false;
    }
    ProfileContainer other = (ProfileContainer) obj;
    return Objects.equals(cpu, other.cpu) && Objects.equals(hugePages, other.hugePages)
        && Objects.equals(memory, other.memory);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
