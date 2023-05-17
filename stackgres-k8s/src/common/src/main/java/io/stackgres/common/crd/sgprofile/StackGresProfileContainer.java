/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.util.Objects;

import javax.validation.Valid;

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
public class StackGresProfileContainer {

  @JsonProperty("cpu")
  private String cpu;

  @JsonProperty("memory")
  private String memory;

  @JsonProperty("hugePages")
  @Valid
  private StackGresProfileHugePages hugePages;

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

  public StackGresProfileHugePages getHugePages() {
    return hugePages;
  }

  public void setHugePages(StackGresProfileHugePages hugePages) {
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
    if (!(obj instanceof StackGresProfileContainer)) {
      return false;
    }
    StackGresProfileContainer other = (StackGresProfileContainer) obj;
    return Objects.equals(cpu, other.cpu) && Objects.equals(hugePages, other.hugePages)
        && Objects.equals(memory, other.memory);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
