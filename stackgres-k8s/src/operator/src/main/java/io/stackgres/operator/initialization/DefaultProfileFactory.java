/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContainerProfile;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.common.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultProfileFactory extends DefaultCustomResourceFactory<StackGresProfile, HasMetadata> {

  @Override
  protected String getDefaultPropertyResourceName(HasMetadata source) {
    return "/instance-profile-default-values.properties";
  }

  @Override
  public StackGresProfile buildResource(HasMetadata resource) {
    StackGresProfile profile = new StackGresProfile();
    profile.getMetadata().setName(getDefaultResourceName(resource));
    profile.getMetadata().setNamespace(resource.getMetadata().getNamespace());

    StackGresProfileSpec spec = buildFromDefaults(resource, StackGresProfileSpec.class);

    profile.setSpec(spec);

    setDefaults(profile);

    return profile;
  }

  public void setDefaults(StackGresProfile resource) {
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

  /**
   * Remove containers and init containers entries that still hold the values
   * derived from the old profile cpu and memory, so that {@link #setDefaults}
   * recomputes them from the new profile cpu and memory. Entries with values
   * that do not match the old derived ones are user customizations and are
   * left untouched.
   */
  public void resetDefaults(StackGresProfile resource, StackGresProfile oldResource) {
    final Optional<BigDecimal> oldCpuLimits = Optional.of(oldResource.getSpec())
        .map(StackGresProfileSpec::getCpu)
        .flatMap(this::tryParseQuantity)
        .map(Quantity::getAmountInBytes);
    final Optional<BigDecimal> oldMemoryLimits = Optional.of(oldResource.getSpec())
        .map(StackGresProfileSpec::getMemory)
        .flatMap(this::tryParseQuantity)
        .map(Quantity::getAmountInBytes);

    Optional.ofNullable(resource.getSpec().getContainers())
        .ifPresent(containers -> resetContainersCpuAndMemory(
            oldCpuLimits, oldMemoryLimits, containerProfiles(), containers));
    Optional.ofNullable(resource.getSpec().getInitContainers())
        .ifPresent(initContainers -> resetContainersCpuAndMemory(
            oldCpuLimits, oldMemoryLimits, initContainerProfiles(), initContainers));

    final Optional<BigDecimal> oldCpuRequests = Optional.of(oldResource.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getCpu)
        .or(() -> Optional.ofNullable(oldResource.getSpec().getCpu()))
        .flatMap(this::tryParseQuantity)
        .map(Quantity::getAmountInBytes);
    final Optional<BigDecimal> oldMemoryRequests = Optional.of(oldResource.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getMemory)
        .or(() -> Optional.ofNullable(oldResource.getSpec().getMemory()))
        .flatMap(this::tryParseQuantity)
        .map(Quantity::getAmountInBytes);

    Optional.ofNullable(resource.getSpec().getRequests())
        .ifPresent(requests -> {
          Optional.ofNullable(requests.getContainers())
              .ifPresent(containers -> resetContainersCpuAndMemory(
                  oldCpuRequests, oldMemoryRequests, containerProfiles(), containers));
          Optional.ofNullable(requests.getInitContainers())
              .ifPresent(initContainers -> resetContainersCpuAndMemory(
                  oldCpuRequests, oldMemoryRequests, initContainerProfiles(), initContainers));
        });
  }

  private List<StackGresContainerProfile> containerProfiles() {
    return Stream.of(StackGresContainer.values())
        .filter(Predicates.not(StackGresContainer.PATRONI::equals))
        .filter(Predicates.not(StackGresContainer.STREAM_CONTROLLER::equals))
        .map(StackGresContainerProfile.class::cast)
        .toList();
  }

  private List<StackGresContainerProfile> initContainerProfiles() {
    return Stream.of(StackGresInitContainer.values())
        .map(StackGresContainerProfile.class::cast)
        .toList();
  }

  private void resetContainersCpuAndMemory(
      Optional<BigDecimal> oldCpu, Optional<BigDecimal> oldMemory,
      List<StackGresContainerProfile> containerProfiles,
      Map<String, StackGresProfileContainer> containers) {
    for (var container : containerProfiles) {
      var containerProfile = containers.get(container.getNameWithPrefix());
      if (containerProfile == null) {
        continue;
      }
      final String oldDerivedCpu = oldCpu
          .map(container.getCpuFormula())
          .map(ResourceUtil::toCpuValue)
          .orElse(null);
      final String oldDerivedMemory = oldMemory
          .map(container.getMemoryFormula())
          .map(ResourceUtil::toMemoryValue)
          .orElse(null);
      if (Objects.equals(containerProfile.getCpu(), oldDerivedCpu)
          && Objects.equals(containerProfile.getMemory(), oldDerivedMemory)) {
        containers.remove(container.getNameWithPrefix());
      }
    }
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
      var containerProfile = initContainers.get(container.getNameWithPrefix());
      if (containerProfile == null) {
        containerProfile = new StackGresProfileContainer();
        setContainerCpu(cpu, container, containerProfile);
        setContainerMemory(memory, container, containerProfile);
        initContainers.put(container.getNameWithPrefix(), containerProfile);
      }
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
