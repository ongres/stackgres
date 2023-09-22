/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import org.jooq.lambda.Seq;

public abstract class AbstractContainerProfileDecorator {

  protected abstract StackGresGroupKind getKind();

  protected void setProfileContainers(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier,
      boolean enableCpuAndMemoryLimits,
      boolean enableCpuRequests, boolean enableMemoryRequests) {
    podSpecSupplier.get()
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForContainer(
            profile, podSpecSupplier, container,
            enableCpuAndMemoryLimits, enableCpuRequests, enableMemoryRequests));
    podSpecSupplier.get()
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForInitContainer(
            profile, podSpecSupplier, container,
            enableCpuAndMemoryLimits, enableCpuRequests, enableMemoryRequests));
  }

  protected void setProfileForContainer(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container,
      boolean enableCpuAndMemoryLimits,
      boolean enableCpuRequests, boolean enableMemoryRequests) {
    var containerRequests = Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getContainers);
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getContainers)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .forEach(entry -> setContainerResources(
            podSpecSupplier, container, containerRequests, entry,
            enableCpuAndMemoryLimits, enableCpuRequests, enableMemoryRequests));
  }

  protected void setProfileForInitContainer(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container,
      boolean enableCpuAndMemoryLimits,
      boolean enableCpuRequests, boolean enableMemoryRequests) {
    var containerRequests = Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getInitContainers);
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getInitContainers)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .forEach(entry -> setContainerResources(
            podSpecSupplier, container, containerRequests, entry,
            enableCpuAndMemoryLimits, enableCpuRequests, enableMemoryRequests));
  }

  private void setContainerResources(Supplier<Optional<PodSpec>> podSpecSupplier,
      Container container, Optional<Map<String, StackGresProfileContainer>> containerRequests,
      Entry<String, StackGresProfileContainer> entry,
      boolean enableCpuAndMemoryLimits,
      boolean enableCpuRequests, boolean enableMemoryRequests) {
    final ResourceRequirements containerResources = new ResourceRequirements();
    final Quantity cpuLimit = new Quantity(Optional.of(entry)
        .map(Map.Entry::getValue)
        .map(StackGresProfileContainer::getCpu)
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find CPU profile configuration for container "
                + entry.getKey())));
    final Quantity memoryLimit = new Quantity(Optional.of(entry)
        .map(Map.Entry::getValue)
        .map(StackGresProfileContainer::getMemory)
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find memory profile configuration for container "
                + entry.getKey())));
    final Quantity cpuRequest = containerRequests
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(containerRequest -> Objects.equals(
            getKind().getName(containerRequest.getKey()), container.getName()))
        .findFirst()
        .map(Map.Entry::getValue)
        .map(StackGresProfileContainer::getCpu)
        .map(Quantity::new)
        .filter(q -> enableCpuRequests)
        .orElse(cpuLimit);
    final Quantity memoryRequest = containerRequests
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(containerRequest -> Objects.equals(
            getKind().getName(containerRequest.getKey()), container.getName()))
        .findFirst()
        .map(Map.Entry::getValue)
        .map(StackGresProfileContainer::getMemory)
        .map(Quantity::new)
        .filter(q -> enableMemoryRequests)
        .orElse(memoryLimit);
    final var requests = new HashMap<String, Quantity>();
    requests.put("cpu", cpuRequest);
    requests.put("memory", memoryRequest);

    if (enableCpuAndMemoryLimits) {
      final var limits = new HashMap<String, Quantity>();
      limits.put("cpu", cpuLimit);
      limits.put("memory", memoryLimit);
      Optional.of(entry.getValue())
          .map(StackGresProfileContainer::getHugePages)
          .map(StackGresProfileHugePages::getHugepages2Mi)
          .map(Quantity::new)
          .ifPresent(quantity -> setHugePages2Mi(
              podSpecSupplier, entry, container, requests, limits, quantity));
      Optional.of(entry.getValue())
          .map(StackGresProfileContainer::getHugePages)
          .map(StackGresProfileHugePages::getHugepages1Gi)
          .map(Quantity::new)
          .ifPresent(quantity -> setHugePages1Gi(
              podSpecSupplier, entry, container, requests, limits, quantity));
      containerResources.setLimits(Map.copyOf(limits));
    }

    containerResources.setRequests(Map.copyOf(requests));

    container.setResources(containerResources);
  }

  private void setHugePages2Mi(Supplier<Optional<PodSpec>> podSpecSupplier,
      Entry<String, StackGresProfileContainer> entry, Container container,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits,
      Quantity quantity) {
    requests.put("hugepages-2Mi", quantity);
    limits.put("hugepages-2Mi", quantity);
    podSpecSupplier.get()
        .ifPresent(spec -> spec.setVolumes(
            getVolumesWithHugePages2Mi(entry, spec)));
    container.setVolumeMounts(
        Seq.seq(Optional.ofNullable(container.getVolumeMounts())
            .stream())
        .flatMap(List::stream)
        .append(new VolumeMountBuilder()
            .withName(StackGresVolume.HUGEPAGES_2M.getName()
                + "-" + entry.getKey())
            .withMountPath(ClusterPath.HUGEPAGES_2M_PATH.path())
            .build())
        .toList());
  }

  private List<Volume> getVolumesWithHugePages2Mi(Entry<String, StackGresProfileContainer> entry,
      PodSpec spec) {
    return Seq.seq(Optional.ofNullable(spec.getVolumes())
        .stream()
        .flatMap(List::stream))
        .append(new VolumeBuilder()
            .withName(StackGresVolume.HUGEPAGES_2M.getName()
                + "-" + entry.getKey())
            .withEmptyDir(new EmptyDirVolumeSourceBuilder()
                .withMedium("HugePages-2Mi")
                .build())
            .build())
        .toList();
  }

  private void setHugePages1Gi(Supplier<Optional<PodSpec>> podSpecSupplier,
      Entry<String, StackGresProfileContainer> entry, Container container,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits,
      Quantity quantity) {
    requests.put("hugepages-1Gi", quantity);
    limits.put("hugepages-1Gi", quantity);
    podSpecSupplier.get()
        .ifPresent(spec -> spec.setVolumes(getVolumesWithHugePages1Gi(entry, spec)));
    container.setVolumeMounts(
        Seq.seq(Optional.ofNullable(container.getVolumeMounts())
            .stream())
        .flatMap(List::stream)
        .append(new VolumeMountBuilder()
            .withName(StackGresVolume.HUGEPAGES_1G.getName()
                + "-" + entry.getKey())
            .withMountPath(ClusterPath.HUGEPAGES_1G_PATH.path())
            .build())
        .toList());
  }

  private List<Volume> getVolumesWithHugePages1Gi(Entry<String, StackGresProfileContainer> entry,
      PodSpec spec) {
    return Seq.seq(Optional.ofNullable(spec.getVolumes())
        .stream()
        .flatMap(List::stream))
        .append(new VolumeBuilder()
            .withName(StackGresVolume.HUGEPAGES_1G.getName()
                + "-" + entry.getKey())
            .withEmptyDir(new EmptyDirVolumeSourceBuilder()
                .withMedium("HugePages-1Gi")
                .build())
            .build())
        .toList();
  }

}
