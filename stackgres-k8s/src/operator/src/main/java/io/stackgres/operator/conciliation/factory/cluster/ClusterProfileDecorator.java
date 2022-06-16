/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresContainers;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class ClusterProfileDecorator implements Decorator<StackGresClusterContext> {

  @Override
  public void decorate(StackGresClusterContext context, Iterable<? extends HasMetadata> resources) {
    Seq.seq(resources)
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .ifPresent(statefulSet -> setProfileContainers(context, statefulSet));
  }

  private void setProfileContainers(StackGresClusterContext context, StatefulSet statefulSet) {
    Optional.of(statefulSet)
        .map(StatefulSet::getSpec)
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getSpec)
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .filter(container -> !Objects.equals(
            container.getName(), StackGresContainers.PATRONI.getName()))
        .forEach(container -> setProfileForContainer(context, statefulSet, container));
    Optional.of(statefulSet)
        .map(StatefulSet::getSpec)
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getSpec)
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForInitContainer(context, statefulSet, container));
  }

  private void setProfileForContainer(StackGresClusterContext context, StatefulSet statefulSet,
      Container container) {
    Optional.of(context.getStackGresProfile().getSpec())
        .map(StackGresProfileSpec::getContainers)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> Objects.equals(container.getName(), entry.getKey()))
        .forEach(entry -> setContainerResources(statefulSet, container, entry));
  }

  private void setProfileForInitContainer(StackGresClusterContext context, StatefulSet statefulSet,
      Container container) {
    Optional.of(context.getStackGresProfile().getSpec())
        .map(StackGresProfileSpec::getInitContainers)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> Objects.equals(container.getName(), entry.getKey()))
        .forEach(entry -> setContainerResources(statefulSet, container, entry));
  }

  private void setContainerResources(StatefulSet statefulSet, Container container,
      Entry<String, StackGresProfileContainer> entry) {
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
            statefulSet, entry, container, requests, limits, quantity));
    Optional.of(entry.getValue())
        .map(StackGresProfileContainer::getHugePages)
        .map(StackGresProfileHugePages::getHugepages1Gi)
        .map(Quantity::new)
        .ifPresent(quantity -> setHugePages1Gi(
            statefulSet, entry, container, requests, limits, quantity));
    containerResources.setRequests(Map.copyOf(requests));
    containerResources.setLimits(Map.copyOf(limits));
    container.setResources(containerResources);
  }

  private void setHugePages2Mi(StatefulSet statefulSet,
      Entry<String, StackGresProfileContainer> entry, Container container,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits,
      Quantity quantity) {
    requests.put("hugepages-2Mi", quantity);
    limits.put("hugepages-2Mi", quantity);
    Optional.of(statefulSet)
        .map(StatefulSet::getSpec)
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getSpec)
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

  private void setHugePages1Gi(StatefulSet statefulSet,
      Entry<String, StackGresProfileContainer> entry, Container container,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits,
      Quantity quantity) {
    requests.put("hugepages-1Gi", quantity);
    limits.put("hugepages-1Gi", quantity);
    Optional.of(statefulSet)
        .map(StatefulSet::getSpec)
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getSpec)
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
