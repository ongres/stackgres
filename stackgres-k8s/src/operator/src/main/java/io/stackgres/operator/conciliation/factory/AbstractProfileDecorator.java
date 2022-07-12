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
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresKind;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import org.jooq.lambda.Seq;

public abstract class AbstractProfileDecorator {

  protected abstract StackGresKind getKind();

  protected void setProfileContainers(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier) {
    podSpecSupplier.get()
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForContainer(profile, podSpecSupplier, container));
    podSpecSupplier.get()
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForInitContainer(profile, podSpecSupplier, container));
  }

  protected void setProfileForContainer(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getContainers)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .forEach(entry -> setContainerResources(podSpecSupplier, container, entry));
  }

  protected void setProfileForInitContainer(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getInitContainers)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .forEach(entry -> setContainerResources(podSpecSupplier, container, entry));
  }

  private void setContainerResources(Supplier<Optional<PodSpec>> podSpecSupplier,
      Container container, Entry<String, StackGresProfileContainer> entry) {
    final ResourceRequirements containerResources = new ResourceRequirements();
    final var requests = new HashMap<String, Quantity>();
    final var limits = new HashMap<String, Quantity>();
    final Quantity cpu = new Quantity(Optional.of(entry)
        .map(Map.Entry::getValue)
        .map(StackGresProfileContainer::getCpu)
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find CPU profile configuration for container "
                + entry.getKey())));
    final Quantity memory = new Quantity(Optional.of(entry)
        .map(Map.Entry::getValue)
        .map(StackGresProfileContainer::getMemory)
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find memory profile configuration for container "
                + entry.getKey())));
    requests.put("cpu", cpu);
    requests.put("memory", memory);
    limits.put("cpu", cpu);
    limits.put("memory", memory);
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
    containerResources.setRequests(Map.copyOf(requests));
    containerResources.setLimits(Map.copyOf(limits));
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
            .withName(PatroniStaticVolume.HUGEPAGES_2M.getVolumeName()
                + "-" + entry.getKey())
            .withMountPath(ClusterStatefulSetPath.HUGEPAGES_2M_PATH.path())
            .build())
        .toList());
  }

  private List<Volume> getVolumesWithHugePages2Mi(Entry<String, StackGresProfileContainer> entry,
      PodSpec spec) {
    return Seq.seq(Optional.ofNullable(spec.getVolumes())
        .stream()
        .flatMap(List::stream))
        .append(new VolumeBuilder()
            .withName(PatroniStaticVolume.HUGEPAGES_2M.getVolumeName()
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
            .withName(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName()
                + "-" + entry.getKey())
            .withMountPath(ClusterStatefulSetPath.HUGEPAGES_1G_PATH.path())
            .build())
        .toList());
  }

  private List<Volume> getVolumesWithHugePages1Gi(Entry<String, StackGresProfileContainer> entry,
      PodSpec spec) {
    return Seq.seq(Optional.ofNullable(spec.getVolumes())
        .stream()
        .flatMap(List::stream))
        .append(new VolumeBuilder()
            .withName(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName()
                + "-" + entry.getKey())
            .withEmptyDir(new EmptyDirVolumeSourceBuilder()
                .withMedium("HugePages-1Gi")
                .build())
            .build())
        .toList();
  }

}
