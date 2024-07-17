/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContainerProfile;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultContainersProfileMutator implements ProfileMutator {

  @Override
  public StackGresProfile mutate(StackGresInstanceProfileReview review, StackGresProfile resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final Optional<BigDecimal> cpuLimits = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getCpu)
          .flatMap(this::tryParseQuantity)
          .map(Quantity::getAmountInBytes);
      final Optional<BigDecimal> memoryLimits = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getMemory)
          .flatMap(this::tryParseQuantity)
          .map(Quantity::getAmountInBytes);

      final var containersLimits = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getContainers)
          .orElseGet(HashMap::new);
      final var initContainersLimits = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getInitContainers)
          .orElseGet(HashMap::new);

      setContainersCpuAndMemory(cpuLimits, memoryLimits, containersLimits);
      resource.getSpec().setContainers(containersLimits);
      setInitContainersCpuAndMemory(cpuLimits, memoryLimits, initContainersLimits);
      resource.getSpec().setInitContainers(initContainersLimits);

      if (resource.getSpec().getRequests() == null) {
        resource.getSpec().setRequests(new StackGresProfileRequests());
        resource.getSpec().getRequests().setCpu(resource.getSpec().getCpu());
        resource.getSpec().getRequests().setMemory(resource.getSpec().getMemory());
      }

      final Optional<BigDecimal> cpuRequests = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getRequests)
          .map(StackGresProfileRequests::getCpu)
          .flatMap(this::tryParseQuantity)
          .map(Quantity::getAmountInBytes);
      final Optional<BigDecimal> memoryRequests = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getRequests)
          .map(StackGresProfileRequests::getMemory)
          .flatMap(this::tryParseQuantity)
          .map(Quantity::getAmountInBytes);

      final var containersRequests = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getRequests)
          .map(StackGresProfileRequests::getContainers)
          .orElseGet(HashMap::new);
      final var initContainersRequests = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getRequests)
          .map(StackGresProfileRequests::getInitContainers)
          .orElseGet(HashMap::new);

      setContainersCpuAndMemory(cpuRequests, memoryRequests, containersRequests);
      resource.getSpec().getRequests().setContainers(containersRequests);
      setInitContainersCpuAndMemory(cpuRequests, memoryRequests, initContainersRequests);
      resource.getSpec().getRequests().setInitContainers(initContainersRequests);
    }

    return resource;
  }

  private void setContainersCpuAndMemory(
      Optional<BigDecimal> cpu, Optional<BigDecimal> memory,
      Map<String, StackGresProfileContainer> containers) {
    for (var container : Stream.of(StackGresContainer.values())
        .filter(Predicates.not(StackGresContainer.PATRONI::equals))
        .filter(Predicates.not(StackGresContainer.STREAM_CONTROLLER::equals))
        .toList()) {
      var containerProfile = containers.get(container.getNameWithPrefix());
      if (containerProfile == null) {
        containerProfile = new StackGresProfileContainer();
        setContainerCpu(cpu, container, containerProfile);
        setContainerMemory(memory, container, containerProfile);
        containers.put(container.getNameWithPrefix(), containerProfile);
      }
    }
  }

  private void setInitContainersCpuAndMemory(
      Optional<BigDecimal> cpu, Optional<BigDecimal> memory,
      Map<String, StackGresProfileContainer> initContainers) {
    for (var container : Stream.of(StackGresInitContainer.values())
        .toList()) {
      var containerProfile = initContainers
          .computeIfAbsent(container.getNameWithPrefix(), k -> new StackGresProfileContainer());
      setContainerCpu(cpu, container, containerProfile);
      setContainerMemory(memory, container, containerProfile);
    }
  }

  private void setContainerCpu(
      Optional<BigDecimal> cpu,
      StackGresContainerProfile container,
      StackGresProfileContainer containerProfile) {
    containerProfile.setCpu(cpu
        .map(container.getCpuFormula())
        .map(ResourceUtil::toCpuValue)
        .orElse(null));
  }

  private void setContainerMemory(
      Optional<BigDecimal> memory,
      StackGresContainerProfile container,
      StackGresProfileContainer containerProfile) {
    containerProfile.setMemory(memory
        .map(container.getMemoryFormula())
        .map(ResourceUtil::toMemoryValue)
        .orElse(null));
  }

  private Optional<Quantity> tryParseQuantity(String quantity) {
    try {
      return Optional.of(new Quantity(quantity));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

}
