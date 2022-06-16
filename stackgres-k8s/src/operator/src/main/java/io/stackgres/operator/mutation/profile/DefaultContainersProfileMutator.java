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
import java.util.function.Function;
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
import io.stackgres.common.StackGresContainers;
import io.stackgres.common.StackGresInitContainers;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultContainersProfileMutator implements ProfileMutator {

  private static final BigDecimal ONE_CPU_IN_MILLICPUS = BigDecimal.valueOf(1000);
  private static final BigDecimal MI_IN_BYTES = Quantity.getAmountInBytes(new Quantity("1Mi"));
  private static final BigDecimal GI_IN_BYTES = Quantity.getAmountInBytes(new Quantity("1Gi"));

  private static final Map<String, Function<BigDecimal, BigDecimal>> CONTAINER_CPU_FORMULAS =
      Map.ofEntries(
          Map.entry(StackGresContainers.PGBOUNCER.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
          Map.entry(StackGresContainers.ENVOY.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(4)))),
          Map.entry(StackGresContainers.POSTGRES_EXPORTER.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
          Map.entry(StackGresContainers.POSTGRES_UTIL.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
          Map.entry(StackGresContainers.FLUENT_BIT.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
          Map.entry(StackGresContainers.CLUSTER_CONTROLLER.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16)))),
          Map.entry(StackGresContainers.FLUENTD.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(4)))),
          Map.entry(StackGresContainers.DISTRIBUTEDLOGS_CONTROLLER.getName(),
              cpu -> BigDecimal.ONE.min(cpu.divide(BigDecimal.valueOf(16))))
          );

  private static final Map<String, Function<BigDecimal, BigDecimal>> CONTAINER_MEMORY_FORMULAS =
      Map.ofEntries(
          Map.entry(StackGresContainers.PGBOUNCER.getName(),
              memory -> BigDecimal.valueOf(64).multiply(MI_IN_BYTES)),
          Map.entry(StackGresContainers.ENVOY.getName(),
              memory -> BigDecimal.valueOf(64).multiply(MI_IN_BYTES)),
          Map.entry(StackGresContainers.POSTGRES_EXPORTER.getName(),
              memory -> BigDecimal.valueOf(8).multiply(MI_IN_BYTES)),
          Map.entry(StackGresContainers.POSTGRES_UTIL.getName(),
              memory -> BigDecimal.valueOf(8).multiply(MI_IN_BYTES)),
          Map.entry(StackGresContainers.FLUENT_BIT.getName(),
              memory -> BigDecimal.valueOf(8).multiply(MI_IN_BYTES)),
          Map.entry(StackGresContainers.CLUSTER_CONTROLLER.getName(),
              memory -> BigDecimal.valueOf(512).multiply(MI_IN_BYTES)),
          Map.entry(StackGresContainers.FLUENTD.getName(),
              memory -> BigDecimal.valueOf(2).multiply(GI_IN_BYTES)),
          Map.entry(StackGresContainers.DISTRIBUTEDLOGS_CONTROLLER.getName(),
              memory -> BigDecimal.valueOf(512).multiply(MI_IN_BYTES))
          );

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

    Stream.of(StackGresContainers.values())
        .filter(Predicates.not(StackGresContainers.PATRONI::equals))
        .map(StackGresContainers::getName)
        .filter(Predicates.not(CONTAINER_CPU_FORMULAS::containsKey))
        .forEach(name -> {
          throw new AssertionError("Container " + name
              + " not defined in CONTAINER_CPU_FORMULAS map");
        });
    Stream.of(StackGresContainers.values())
        .filter(Predicates.not(StackGresContainers.PATRONI::equals))
        .map(StackGresContainers::getName)
        .filter(Predicates.not(CONTAINER_MEMORY_FORMULAS::containsKey))
        .forEach(name -> {
          throw new AssertionError("Container " + name
              + " not defined in CONTAINER_MEMORY_FORMULAS map");
        });
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
          .map(StackGresProfileSpec::getCpu)
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
    for (var name : Stream.of(StackGresContainers.values())
        .filter(Predicates.not(StackGresContainers.PATRONI::equals))
        .map(StackGresContainers::getName)
        .toList()) {
      var containerProfile = Optional.of(containers
          .computeIfAbsent(name, k -> new StackGresProfileContainer()));
      boolean setContainerCpu = setContainerCpu(cpu, name, containerProfile);
      boolean setContainerMemory = setContainerMemory(memory, name, containerProfile);
      result = result || setContainerCpu || setContainerMemory;
    }
    return result;
  }

  private boolean setContainerCpu(BigDecimal cpu, String name,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithCpu = containerProfile
        .filter(profile -> Objects.isNull(profile.getCpu()));
    containerProfileWithCpu.ifPresent(profile -> profile.setCpu(
        toCpuValue(CONTAINER_CPU_FORMULAS.get(name).apply(cpu))));
    return containerProfileWithCpu.isPresent();
  }

  private boolean setContainerMemory(BigDecimal memory, String name,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithMemory = containerProfile
        .filter(profile -> Objects.isNull(profile.getMemory()));
    containerProfileWithMemory.ifPresent(profile -> profile.setMemory(
        toMemoryValue(CONTAINER_MEMORY_FORMULAS.get(name).apply(memory))));
    return containerProfileWithMemory.isPresent();
  }

  private boolean setInitContainersCpuAndMemory(
      BigDecimal cpu, BigDecimal memory,
      Map<String, StackGresProfileContainer> initContainers) {
    boolean result = false;
    for (var name : Stream.of(StackGresInitContainers.values())
        .map(StackGresInitContainers::getName)
        .toList()) {
      var containerProfile = Optional.of(initContainers
          .computeIfAbsent(name, k -> new StackGresProfileContainer()));
      boolean setInitContainerCpu = setInitContainerCpu(cpu, containerProfile);
      boolean setInitContainerMemory = setInitContainerMemory(memory, containerProfile);
      result = result || setInitContainerCpu || setInitContainerMemory;
    }
    return result;
  }

  private boolean setInitContainerCpu(BigDecimal cpu,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithCpu = containerProfile
        .filter(profile -> Objects.isNull(profile.getCpu()));
    containerProfileWithCpu.ifPresent(profile -> profile.setCpu(
        toCpuValue(cpu)));
    return containerProfileWithCpu.isPresent();
  }

  private boolean setInitContainerMemory(BigDecimal memory,
      Optional<StackGresProfileContainer> containerProfile) {
    var containerProfileWithMemory = containerProfile
        .filter(profile -> Objects.isNull(profile.getMemory()));
    containerProfileWithMemory.ifPresent(profile -> profile.setMemory(
        toMemoryValue(memory)));
    return containerProfileWithMemory.isPresent();
  }

  private String toCpuValue(BigDecimal value) {
    if (value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0) {
      return value
          .multiply(ONE_CPU_IN_MILLICPUS)
          .setScale(0, RoundingMode.CEILING).toString() + "m";
    }
    return value
        .setScale(0, RoundingMode.CEILING).toString();
  }

  private String toMemoryValue(BigDecimal value) {
    if (value.remainder(GI_IN_BYTES).compareTo(BigDecimal.ZERO) == 0) {
      return value.divide(GI_IN_BYTES)
          .setScale(0, RoundingMode.CEILING).toString() + "Gi";
    }
    return value.divide(MI_IN_BYTES)
        .setScale(0, RoundingMode.CEILING).toString() + "Mi";
  }

}
