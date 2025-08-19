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
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.crd.CustomEnvFromSource;
import io.stackgres.common.crd.CustomEnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import org.jooq.lambda.Seq;

public abstract class AbstractContainerCustomEnvDecorator {

  protected abstract StackGresGroupKind getKind();

  protected void setCustomEnvContainers(StackGresCluster cluster,
      Supplier<Optional<PodSpec>> podSpecSupplier) {
    podSpecSupplier.get()
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setCustomEnvForContainer(
            cluster, podSpecSupplier, container));
    podSpecSupplier.get()
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setCustomEnvForInitContainer(
            cluster, podSpecSupplier, container));
  }

  protected void setCustomEnvForContainer(StackGresCluster cluster,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container) {
    Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomEnv)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .flatMap(entry -> Optional.ofNullable(entry.getValue())
            .stream()
            .flatMap(List::stream)
            .map(value -> Map.entry(entry.getKey(), value)))
        .forEach(entry -> setCustomEnv(
            podSpecSupplier, container, entry));
    Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomEnvFrom)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .flatMap(entry -> Optional.ofNullable(entry.getValue())
            .stream()
            .flatMap(List::stream)
            .map(value -> Map.entry(entry.getKey(), value)))
        .forEach(entry -> setCustomEnvFrom(
            podSpecSupplier, container, entry));
  }

  protected void setCustomEnvForInitContainer(StackGresCluster cluster,
      Supplier<Optional<PodSpec>> podSpecSupplier, Container container) {
    Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomInitEnv)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .flatMap(entry -> Optional.ofNullable(entry.getValue())
            .stream()
            .flatMap(List::stream)
            .map(value -> Map.entry(entry.getKey(), value)))
        .forEach(entry -> setCustomEnv(
            podSpecSupplier, container, entry));
    Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomInitEnvFrom)
        .map(Map::entrySet)
        .stream()
        .flatMap(Collection::stream)
        .filter(entry -> getKind().hasPrefix(entry.getKey()))
        .filter(entry -> Objects.equals(
            container.getName(),
            getKind().getName(entry.getKey())))
        .flatMap(entry -> Optional.ofNullable(entry.getValue())
            .stream()
            .flatMap(List::stream)
            .map(value -> Map.entry(entry.getKey(), value)))
        .forEach(entry -> setCustomEnvFrom(
            podSpecSupplier, container, entry));
  }

  private void setCustomEnv(Supplier<Optional<PodSpec>> podSpecSupplier,
      Container container,
      Entry<String, CustomEnvVar> entry) {
    container.setEnv(
        Optional.ofNullable(container.getEnv())
        .or(() -> Optional.of(List.of()))
        .stream()
        .flatMap(list -> Seq.seq(list)
            .append(Seq.<EnvVar>of(entry.getValue())))
        .toList());
  }

  private void setCustomEnvFrom(Supplier<Optional<PodSpec>> podSpecSupplier,
      Container container,
      Entry<String, CustomEnvFromSource> entry) {
    container.setEnvFrom(
        Optional.ofNullable(container.getEnvFrom())
        .or(() -> Optional.of(List.of()))
        .stream()
        .flatMap(list -> Seq.seq(list)
            .append(Seq.<EnvFromSource>of(entry.getValue())))
        .toList());
  }

}
