/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContainerProfile;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultContainersProfileMutator implements ProfileMutator {

  private final DefaultCustomResourceFactory<StackGresProfile> factory;

  private StackGresProfile defaultProfile;

  @Inject
  public DefaultContainersProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile> factory) {
    this.factory = factory;
  }

  @PostConstruct
  public void init() throws NoSuchFieldException {
    defaultProfile = factory.buildResource();
  }

  @Override
  public StackGresProfile mutate(SgProfileReview review, StackGresProfile resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final BigDecimal cpu = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getCpu)
          .or(() -> Optional.of(defaultProfile.getSpec().getCpu()))
          .flatMap(this::tryParseQuantity)
          .map(Quantity::getAmountInBytes)
          .orElse(BigDecimal.ZERO);
      final BigDecimal memory = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getMemory)
          .or(() -> Optional.of(defaultProfile.getSpec().getMemory()))
          .flatMap(this::tryParseQuantity)
          .map(Quantity::getAmountInBytes)
          .orElse(BigDecimal.ZERO);

      final var containers = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getContainers)
          .orElseGet(HashMap::new);
      final var initContainers = Optional.of(resource.getSpec())
          .map(StackGresProfileSpec::getInitContainers)
          .orElseGet(HashMap::new);

      setContainersCpuAndMemory(resource, cpu, memory, containers);
      setInitContainersCpuAndMemory(resource, cpu, memory, initContainers);
    }

    return resource;
  }

  private void setContainersCpuAndMemory(
      StackGresProfile resource,
      BigDecimal cpu, BigDecimal memory,
      Map<String, StackGresProfileContainer> containers) {
    for (var container : Stream.of(StackGresContainer.values())
        .filter(Predicates.not(StackGresContainer.PATRONI::equals))
        .toList()) {
      var containerProfile = Optional.of(containers
          .computeIfAbsent(
              container.getNameWithPrefix(), k -> new StackGresProfileContainer()));
      setContainerCpu(cpu, container, containerProfile);
      setContainerMemory(memory, container, containerProfile);
    }
    resource.getSpec().setContainers(containers);
  }

  private void setInitContainersCpuAndMemory(
      StackGresProfile resource,
      BigDecimal cpu, BigDecimal memory,
      Map<String, StackGresProfileContainer> initContainers) {
    for (var container : Stream.of(StackGresInitContainer.values())
        .toList()) {
      var containerProfile = Optional.of(initContainers
          .computeIfAbsent(container.getNameWithPrefix(), k -> new StackGresProfileContainer()));
      setContainerCpu(cpu, container, containerProfile);
      setContainerMemory(memory, container, containerProfile);
    }
    resource.getSpec().setInitContainers(initContainers);
  }

  private void setContainerCpu(BigDecimal cpu, StackGresContainerProfile container,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithCpu = containerProfile
        .filter(profile -> Objects.isNull(profile.getCpu()));
    containerProfileWithCpu.ifPresent(profile -> profile.setCpu(
        toCpuValue(container.getCpuFormula().apply(cpu))));
  }

  private void setContainerMemory(BigDecimal memory, StackGresContainerProfile container,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithMemory = containerProfile
        .filter(profile -> Objects.isNull(profile.getMemory()));
    containerProfileWithMemory.ifPresent(profile -> profile.setMemory(
        toMemoryValue(container.getMemoryFormula().apply(memory))));
  }

  private String toCpuValue(BigDecimal value) {
    if (value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0) {
      return value
          .multiply(ResourceUtil.MILLICPU_MULTIPLIER)
          .setScale(0, RoundingMode.CEILING).toString() + "m";
    }
    return value
        .setScale(0, RoundingMode.CEILING).toString();
  }

  private String toMemoryValue(BigDecimal value) {
    if (value.remainder(ResourceUtil.GIBIBYTES).compareTo(BigDecimal.ZERO) == 0) {
      return value.divide(ResourceUtil.GIBIBYTES)
          .setScale(0, RoundingMode.CEILING).toString() + "Gi";
    }
    return value.divide(ResourceUtil.MEBIBYTES)
        .setScale(0, RoundingMode.CEILING).toString() + "Mi";
  }

  private Optional<Quantity> tryParseQuantity(String quantity) {
    try {
      return Optional.of(new Quantity(quantity));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

}
