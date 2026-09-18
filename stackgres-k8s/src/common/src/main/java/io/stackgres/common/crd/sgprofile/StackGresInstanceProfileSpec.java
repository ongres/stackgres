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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresInstanceProfileSpec extends AdditionalProperties {

  private String cpu;

  private String memory;

  @Valid
  private StackGresInstanceProfileHugePages hugePages;

  private Map<String, @Valid StackGresInstanceProfileContainer> containers;

  private Map<String, @Valid StackGresInstanceProfileContainer> initContainers;

  @Valid
  private StackGresInstanceProfileRequests requests;

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

  public StackGresInstanceProfileHugePages getHugePages() {
    return hugePages;
  }

  public void setHugePages(StackGresInstanceProfileHugePages hugePages) {
    this.hugePages = hugePages;
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

  public StackGresInstanceProfileRequests getRequests() {
    return requests;
  }

  public void setRequests(StackGresInstanceProfileRequests requests) {
    this.requests = requests;
  }

  @Override
  public int hashCode() {
    return Objects.hash(containers, cpu, hugePages, initContainers, memory, requests);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresInstanceProfileSpec)) {
      return false;
    }
    StackGresInstanceProfileSpec other = (StackGresInstanceProfileSpec) obj;
    return Objects.equals(containers, other.containers) && Objects.equals(cpu, other.cpu)
        && Objects.equals(hugePages, other.hugePages)
        && Objects.equals(initContainers, other.initContainers)
        && Objects.equals(memory, other.memory) && Objects.equals(requests, other.requests);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
