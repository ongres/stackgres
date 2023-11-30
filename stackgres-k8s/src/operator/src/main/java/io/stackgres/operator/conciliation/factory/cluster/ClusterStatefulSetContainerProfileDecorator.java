/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterStatefulSetContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresClusterContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.CLUSTER;
  }

  @Override
  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  public HasMetadata decorate(StackGresClusterContext context, HasMetadata resource) {
    if (context.calculateDisableClusterResourceRequirements()
        && context.calculateDisablePatroniResourceRequirements()) {
      return resource;
    }

    if (resource instanceof StatefulSet statefulSet) {
      if (!context.calculateDisableClusterResourceRequirements()) {
        setProfileContainers(context.getProfile(),
            () -> Optional.of(statefulSet)
            .map(StatefulSet::getSpec)
            .map(StatefulSetSpec::getTemplate)
            .map(PodTemplateSpec::getSpec),
            Optional.ofNullable(context.getSource().getSpec().getPods().getResources())
            .map(StackGresClusterResources::getEnableClusterLimitsRequirements)
            .orElse(false));
      }
      if (!context.calculateDisablePatroniResourceRequirements()) {
        setPatroniContainerResources(context, statefulSet);
      }
    }

    return resource;
  }

  private void setPatroniContainerResources(
      StackGresClusterContext context,
      StatefulSet statefulSet) {
    final var profile = context.getProfile();

    final ResourceRequirements patroniResources = new ResourceRequirements();
    final Quantity cpuLimit = Optional.ofNullable(profile.getSpec().getCpu())
        .map(Quantity::new).orElse(null);
    final Quantity memoryLimit = Optional.ofNullable(profile.getSpec().getMemory())
        .map(Quantity::new).orElse(null);
    final var limits = new HashMap<String, Quantity>();
    if (cpuLimit != null) {
      limits.put("cpu", cpuLimit);
    }
    if (memoryLimit != null) {
      limits.put("memory", memoryLimit);
    }
    boolean disableResourcesRequestsSplitFromTotal =
        Optional.of(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getResources)
        .map(StackGresClusterResources::getDisableResourcesRequestsSplitFromTotal)
        .orElse(false);
    final Quantity cpuRequest = Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getCpu)
        .map(Quantity::new)
        .map(Quantity::getNumericalAmount)
        .map(amount -> Optional.of(statefulSet)
            .filter(sts -> !disableResourcesRequestsSplitFromTotal)
            .map(StatefulSet::getSpec)
            .map(StatefulSetSpec::getTemplate)
            .map(PodTemplateSpec::getSpec)
            .map(PodSpec::getContainers)
            .map(containers -> containers.stream()
                .map(container -> Optional.of(container)
                    .map(Container::getResources)
                    .map(ResourceRequirements::getRequests)
                    .map(requests -> requests.get("cpu")))
                .flatMap(Optional::stream)
                .map(Quantity::getNumericalAmount)
                .reduce(
                    amount,
                    (calculated, containerAmount) -> calculated.subtract(containerAmount),
                    (u, v) -> v))
            .orElse(amount))
        .map(ResourceUtil::toCpuValue)
        .map(Quantity::new)
        .orElse(null);
    final Quantity memoryRequest = Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getRequests)
        .map(StackGresProfileRequests::getMemory)
        .map(Quantity::new)
        .map(Quantity::getNumericalAmount)
        .map(amount -> Optional.of(statefulSet)
            .filter(sts -> !disableResourcesRequestsSplitFromTotal)
            .map(StatefulSet::getSpec)
            .map(StatefulSetSpec::getTemplate)
            .map(PodTemplateSpec::getSpec)
            .map(PodSpec::getContainers)
            .map(containers -> containers.stream()
                .map(container -> Optional.of(container)
                    .map(Container::getResources)
                    .map(ResourceRequirements::getRequests)
                    .map(requests -> requests.get("memory")))
            .flatMap(Optional::stream)
            .map(Quantity::getNumericalAmount)
            .reduce(
                amount,
                (calculated, containerAmount) -> calculated.subtract(containerAmount),
                (u, v) -> v))
            .orElse(amount))
        .map(ResourceUtil::toMemoryValue)
        .map(Quantity::new)
        .orElse(null);
    final var requests = new HashMap<String, Quantity>();
    if (cpuRequest != null) {
      requests.put("cpu", cpuRequest);
    }
    if (memoryRequest != null) {
      requests.put("memory", memoryRequest);
    }
    setHugePages1Gi(profile, requests, limits);
    setHugePages2Mi(profile, requests, limits);
    patroniResources.setRequests(Map.copyOf(requests));
    patroniResources.setLimits(Map.copyOf(limits));
    Optional.of(statefulSet)
        .map(StatefulSet::getSpec)
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getSpec)
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .filter(container -> Objects.equals(
            container.getName(), StackGresContainer.PATRONI.getName()))
        .findFirst()
        .ifPresent(patroniContainer -> patroniContainer.setResources(patroniResources));
  }

  private void setHugePages2Mi(StackGresProfile profile,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getHugePages)
        .map(StackGresProfileHugePages::getHugepages2Mi)
        .map(Quantity::new)
        .ifPresent(quantity -> {
          requests.put("hugepages-2Mi", quantity);
          limits.put("hugepages-2Mi", quantity);
        });
  }

  private void setHugePages1Gi(StackGresProfile profile,
      final HashMap<String, Quantity> requests, final HashMap<String, Quantity> limits) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getHugePages)
        .map(StackGresProfileHugePages::getHugepages1Gi)
        .map(Quantity::new)
        .ifPresent(quantity -> {
          requests.put("hugepages-1Gi", quantity);
          limits.put("hugepages-1Gi", quantity);
        });
  }

  @Override
  protected void setProfileContainers(StackGresProfile profile,
      Supplier<Optional<PodSpec>> podSpecSupplier,
      boolean enableCpuAndMemoryLimits) {
    podSpecSupplier.get()
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .filter(container -> !Objects.equals(
            container.getName(), StackGresContainer.PATRONI.getName()))
        .forEach(container -> setProfileForContainer(profile, podSpecSupplier, container,
            enableCpuAndMemoryLimits));
    podSpecSupplier.get()
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForInitContainer(profile, podSpecSupplier, container,
            enableCpuAndMemoryLimits));
  }

}
