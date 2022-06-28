/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresProfileSpec {

  @JsonProperty("cpu")
  @NotBlank(message = "cpu must be provided")
  private String cpu;

  @JsonProperty("memory")
  @NotBlank(message = "memory must be provided")
  private String memory;

  @JsonProperty("hugePages")
  @Valid
  private StackGresProfileHugePages hugePages;

  @JsonProperty("containers")
  @Valid
  private Map<String, StackGresProfileContainer> containers;

  @JsonProperty("initContainers")
  @Valid
  private Map<String, StackGresProfileContainer> initContainers;

  @ReferencedField("memory")
  interface Memory extends FieldReference {
  }

  @ReferencedField("containers")
  interface Containers extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "memory can not be less than the sum of all hugepages",
      payload = {Memory.class})
  public boolean isMemoryGreaterOrEqualsToSumOfHugePages() {
    return hugePages == null
        || Optional.ofNullable(memory)
        .map(Quantity::new)
        .map(Quantity::getAmountInBytes)
        .orElse(BigDecimal.ZERO).subtract(
            Optional.ofNullable(hugePages.getHugepages2Mi())
            .map(Quantity::new)
            .map(Quantity::getAmountInBytes)
            .orElse(BigDecimal.ZERO)
            .add(Optional.ofNullable(hugePages.getHugepages1Gi())
            .map(Quantity::new)
            .map(Quantity::getAmountInBytes)
            .orElse(BigDecimal.ZERO))).compareTo(BigDecimal.ZERO) >= 0;
  }

  @JsonIgnore
  @AssertTrue(message = "patroni container can not be set in containers since it is configured"
      + " using the cpu and memory fields",
      payload = {Containers.class})
  public boolean isPatroniContainerNotInContainers() {
    return Optional.ofNullable(containers)
        .map(Map::keySet)
        .stream()
        .flatMap(Collection::stream)
        .noneMatch(StackGresContainer.PATRONI.getName()::equals);
  }

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
    return Objects.hash(containers, cpu, hugePages, initContainers, memory);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresProfileSpec)) {
      return false;
    }
    StackGresProfileSpec other = (StackGresProfileSpec) obj;
    return Objects.equals(containers, other.containers) && Objects.equals(cpu, other.cpu)
        && Objects.equals(hugePages, other.hugePages)
        && Objects.equals(initContainers, other.initContainers)
        && Objects.equals(memory, other.memory);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
