/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
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

  private JsonPointer containersPointer;
  private JsonPointer initContainersPointer;

  @Inject
  public DefaultContainersProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile> factory) {
    this.factory = factory;
  }

  @PostConstruct
  public void init() throws NoSuchFieldException {
    defaultProfile = factory.buildResource();

    String containersJson = getJsonMappingField("containers",
        StackGresProfileSpec.class);
    String initContainersJson = getJsonMappingField("initContainers",
        StackGresProfileSpec.class);

    containersPointer = SPEC_POINTER
        .append(containersJson);
    initContainersPointer = SPEC_POINTER
        .append(initContainersJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(SgProfileReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresProfile profile = review.getRequest().getObject();
      final ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

      final BigDecimal cpu = Optional.of(profile.getSpec())
          .map(StackGresProfileSpec::getCpu)
          .or(() -> Optional.of(defaultProfile.getSpec().getCpu()))
          .map(Quantity::new)
          .map(Quantity::getAmountInBytes)
          .orElse(BigDecimal.ZERO);
      final BigDecimal memory = Optional.of(profile.getSpec())
          .map(StackGresProfileSpec::getMemory)
          .or(() -> Optional.of(defaultProfile.getSpec().getMemory()))
          .map(Quantity::new)
          .map(Quantity::getAmountInBytes)
          .orElse(BigDecimal.ZERO);

      final var containers = Optional.of(profile.getSpec())
          .map(StackGresProfileSpec::getContainers)
          .orElseGet(HashMap::new);
      final var initContainers = Optional.of(profile.getSpec())
          .map(StackGresProfileSpec::getInitContainers)
          .orElseGet(HashMap::new);

      boolean setContainersCpuAndMemory = setContainersCpuAndMemory(
          cpu, memory, containers);
      boolean setInitContainersCpuAndMemory = setInitContainersCpuAndMemory(
          cpu, memory, initContainers);

      if (setContainersCpuAndMemory) {
        if (profile.getSpec().getContainers() == null) {
          operations.add(new AddOperation(
              containersPointer, FACTORY.pojoNode(containers)));
        } else {
          operations.add(new ReplaceOperation(
              containersPointer, FACTORY.pojoNode(containers)));
        }
      }

      if (setInitContainersCpuAndMemory) {
        if (profile.getSpec().getInitContainers() == null) {
          operations.add(new AddOperation(
              initContainersPointer, FACTORY.pojoNode(initContainers)));
        } else {
          operations.add(new ReplaceOperation(
              initContainersPointer, FACTORY.pojoNode(initContainers)));
        }
      }

      return operations.build();
    }

    return List.of();
  }

  private boolean setContainersCpuAndMemory(
      BigDecimal cpu, BigDecimal memory,
      Map<String, StackGresProfileContainer> containers) {
    boolean result = false;
    for (var container : Stream.of(StackGresContainer.values())
        .filter(Predicates.not(StackGresContainer.PATRONI::equals))
        .toList()) {
      var containerProfile = Optional.of(containers
          .computeIfAbsent(
              container.getNameWithPrefix(), k -> new StackGresProfileContainer()));
      boolean setContainerCpu = setContainerCpu(cpu, container, containerProfile);
      boolean setContainerMemory = setContainerMemory(memory, container, containerProfile);
      result = result || setContainerCpu || setContainerMemory;
    }
    return result;
  }

  private boolean setInitContainersCpuAndMemory(
      BigDecimal cpu, BigDecimal memory,
      Map<String, StackGresProfileContainer> initContainers) {
    boolean result = false;
    for (var container : Stream.of(StackGresInitContainer.values())
        .toList()) {
      var containerProfile = Optional.of(initContainers
          .computeIfAbsent(container.getNameWithPrefix(), k -> new StackGresProfileContainer()));
      boolean setInitContainerCpu = setContainerCpu(cpu, container, containerProfile);
      boolean setInitContainerMemory = setContainerMemory(memory, container, containerProfile);
      result = result || setInitContainerCpu || setInitContainerMemory;
    }
    return result;
  }

  private boolean setContainerCpu(BigDecimal cpu, StackGresContainerProfile container,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithCpu = containerProfile
        .filter(profile -> Objects.isNull(profile.getCpu()));
    containerProfileWithCpu.ifPresent(profile -> profile.setCpu(
        toCpuValue(container.getCpuFormula().apply(cpu))));
    return containerProfileWithCpu.isPresent();
  }

  private boolean setContainerMemory(BigDecimal memory, StackGresContainerProfile container,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithMemory = containerProfile
        .filter(profile -> Objects.isNull(profile.getMemory()));
    containerProfileWithMemory.ifPresent(profile -> profile.setMemory(
        toMemoryValue(container.getMemoryFormula().apply(memory))));
    return containerProfileWithMemory.isPresent();
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

}
