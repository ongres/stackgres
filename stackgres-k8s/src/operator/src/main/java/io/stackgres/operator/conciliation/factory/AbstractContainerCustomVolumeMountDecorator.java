/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.crd.CustomVolumeMount;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import org.jooq.lambda.Seq;

public abstract class AbstractContainerCustomVolumeMountDecorator {

  protected abstract StackGresGroupKind getKind();

  protected void setCustomVolumeMountContainers(StackGresCluster cluster,
      Supplier<Optional<PodSpec>> podSpecSupplier) {
    podSpecSupplier.get()
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setCustomVolumeMountForContainer(
            cluster, podSpecSupplier, container));
    podSpecSupplier.get()
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setCustomVolumeMountForInitContainer(
            cluster, podSpecSupplier, container));
  }

  protected void setCustomVolumeMountForContainer(StackGresCluster cluster,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container) {
    Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomVolumeMounts)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .forEach(entry -> setCustomVolumeMount(
            podSpecSupplier, container, entry));
  }

  protected void setCustomVolumeMountForInitContainer(StackGresCluster cluster,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container) {
    Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomInitVolumeMounts)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .forEach(entry -> setCustomVolumeMount(
            podSpecSupplier, container, entry));
  }

  private void setCustomVolumeMount(Supplier<Optional<PodSpec>> podSpecSupplier,
      Container container,
      Entry<String, CustomVolumeMount> entry) {
    container.setVolumeMounts(
        Optional.ofNullable(container.getVolumeMounts())
        .or(() -> Optional.of(List.of()))
        .stream()
        .flatMap(list -> Seq.seq(list)
            .append(Seq.<VolumeMount>of(entry.getValue())))
        .toList());
  }

}
