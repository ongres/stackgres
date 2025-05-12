/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ClusterStatefulSetContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresClusterContext> {

  private final DefaultProfileFactory defaultProfileFactory;

  public ClusterStatefulSetContainerProfileDecorator(DefaultProfileFactory defaultProfileFactory) {
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.CLUSTER;
  }

  @Override
  public HasMetadata decorate(StackGresClusterContext context, HasMetadata resource) {
    if (context.calculateDisableClusterResourceRequirements()
        && context.calculateDisablePatroniResourceRequirements()) {
      return resource;
    }

    final StackGresProfile profile = context.getProfile()
        .orElseGet(() -> defaultProfileFactory.buildResource(context.getSource()));

    if (resource instanceof StatefulSet statefulSet) {
      if (!context.calculateDisableClusterResourceRequirements()) {
        setProfileContainers(
            profile,
            Optional.ofNullable(context.getCluster().getSpec().getPods().getResources()),
            Optional.of(statefulSet)
            .map(StatefulSet::getSpec)
            .map(StatefulSetSpec::getTemplate)
            .map(PodTemplateSpec::getSpec));
      }
      if (!context.calculateDisablePatroniResourceRequirements()) {
        setPatroniContainerResources(
            profile,
            Optional.ofNullable(context.getCluster().getSpec().getPods().getResources()),
            statefulSet);
      }
    }

    return resource;
  }

  private void setPatroniContainerResources(
      StackGresProfile profile,
      Optional<StackGresClusterResources> resources,
      StatefulSet statefulSet) {
    final Optional<ResourceRequirements> containerRequestRequirements =
        resources
        .map(StackGresClusterResources::getContainers)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(containerRequest -> getKind().hasPrefix(containerRequest.getKey()))
        .filter(containerRequest -> Objects.equals(
            getKind().getName(containerRequest.getKey()), StackGresContainer.PATRONI.getName()))
        .findFirst()
        .map(Map.Entry::getValue);
    final ResourceRequirements patroniResources =
        containerRequestRequirements
        .orElseGet(ResourceRequirements::new);
    final Quantity cpuLimit =
        Optional.ofNullable(profile.getSpec().getCpu())
        .map(Quantity::new)
        .orElse(null);
    final Quantity memoryLimit =
        Optional.ofNullable(profile.getSpec().getMemory())
        .map(Quantity::new)
        .orElse(null);
    final HashMap<String, Quantity> limits =
        Optional.of(patroniResources)
        .map(ResourceRequirements::getRequests)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
    if (cpuLimit != null
        && !limits.containsKey("cpu")) {
      limits.put("cpu", cpuLimit);
    }
    if (memoryLimit != null
        && !limits.containsKey("memory")) {
      limits.put("memory", memoryLimit);
    }
    final boolean disableResourcesRequestsSplitFromTotal =
        resources
        .map(StackGresClusterResources::getDisableResourcesRequestsSplitFromTotal)
        .orElse(false);
    final boolean failWhenTotalIsHigher =
        resources
        .map(StackGresClusterResources::getFailWhenTotalIsHigher)
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
    final HashMap<String, Quantity> requests =
        Optional.of(patroniResources)
        .map(ResourceRequirements::getRequests)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
    if (cpuRequest != null
        && !requests.containsKey("cpu")) {
      if (cpuRequest.getNumericalAmount().compareTo(BigDecimal.ZERO) < 0) {
        if (failWhenTotalIsHigher) {
          throw new IllegalArgumentException(
              "Can not set a negative quantity " + cpuRequest.getAmount() + cpuRequest.getFormat()
              + " for cpu requests of patroni container, please increase the `.spec.requests.cpu`"
              + " of the referenced SGInstanceProfile");
        }
        requests.put("cpu", new Quantity("0"));
      } else {
        requests.put("cpu", cpuRequest);
      }
    }
    if (memoryRequest != null
        && !requests.containsKey("memory")) {
      if (memoryRequest.getNumericalAmount().compareTo(BigDecimal.ZERO) < 0) {
        if (failWhenTotalIsHigher) {
          throw new IllegalArgumentException(
              "Can not set a negative quantity " + memoryRequest.getAmount() + memoryRequest.getFormat()
              + " for memory requests of patroni container, please increase the `.spec.requests.memory`"
              + " of the referenced SGInstanceProfile");
        }
        requests.put("memory", new Quantity("0"));
      } else {
        requests.put("memory", memoryRequest);
      }
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

  private void setHugePages2Mi(
      StackGresProfile profile,
      final HashMap<String, Quantity> requests,
      final HashMap<String, Quantity> limits) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getHugePages)
        .map(StackGresProfileHugePages::getHugepages2Mi)
        .map(Quantity::new)
        .ifPresent(quantity -> {
          if (!requests.containsKey("hugepages-2Mi")) {
            requests.put("hugepages-2Mi", quantity);
          }
          if (!limits.containsKey("hugepages-2Mi")) {
            limits.put("hugepages-2Mi", quantity);
          }
        });
  }

  private void setHugePages1Gi(
      StackGresProfile profile,
      final HashMap<String, Quantity> requests,
      final HashMap<String, Quantity> limits) {
    Optional.of(profile.getSpec())
        .map(StackGresProfileSpec::getHugePages)
        .map(StackGresProfileHugePages::getHugepages1Gi)
        .map(Quantity::new)
        .ifPresent(quantity -> {
          if (!requests.containsKey("hugepages-1Gi")) {
            requests.put("hugepages-1Gi", quantity);
          }
          if (!limits.containsKey("hugepages-1Gi")) {
            limits.put("hugepages-1Gi", quantity);
          }
        });
  }

  @Override
  protected void setProfileContainers(
      StackGresProfile profile,
      Optional<StackGresClusterResources> resources,
      Optional<PodSpec> podSpec) {
    podSpec
        .map(PodSpec::getContainers)
        .stream()
        .flatMap(List::stream)
        .filter(container -> !Objects.equals(
            container.getName(), StackGresContainer.PATRONI.getName()))
        .forEach(container -> setProfileForContainer(
            profile, resources, podSpec, container));
    podSpec
        .map(PodSpec::getInitContainers)
        .stream()
        .flatMap(List::stream)
        .forEach(container -> setProfileForInitContainer(
            profile, resources, podSpec, container));
  }

}
