/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainerBuilder;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractProfileDecoratorTestCase;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetContainerProfileDecoratorTest extends AbstractProfileDecoratorTestCase {

  private static final StackGresGroupKind KIND = StackGresGroupKind.CLUSTER;

  private final ClusterStatefulSetContainerProfileDecorator profileDecorator =
      new ClusterStatefulSetContainerProfileDecorator();

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  private StatefulSet statefulSet;

  private List<HasMetadata> resources;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeXs().get();

    final ObjectMeta metadata = cluster.getMetadata();
    metadata.getAnnotations().put(StackGresContext.VERSION_KEY,
        StackGresProperty.OPERATOR_VERSION.getString());
    resources = KubernetessMockResourceGenerationUtil
        .buildResources(metadata.getName(), metadata.getNamespace());
    statefulSet = resources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .orElseThrow();
    profile.getSpec().setContainers(new HashMap<>());
    profile.getSpec().setInitContainers(new HashMap<>());
    Seq.seq(statefulSet.getSpec().getTemplate().getSpec().getContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    Seq.seq(statefulSet.getSpec().getTemplate().getSpec().getInitContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getInitContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    profile.getSpec().setRequests(new StackGresProfileRequests());
    profile.getSpec().getRequests().setCpu(new Random().nextInt(32000) + "m");
    profile.getSpec().getRequests().setMemory(new Random().nextInt(32) + "Gi");
    profile.getSpec().getRequests().setContainers(new HashMap<>());
    profile.getSpec().getRequests().setInitContainers(new HashMap<>());
    Seq.seq(statefulSet.getSpec().getTemplate().getSpec().getContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getRequests().getContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    Seq.seq(statefulSet.getSpec().getTemplate().getSpec().getInitContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getRequests().getInitContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    StackGresProfileContainer containerProfile = new StackGresProfileContainer();
    containerProfile.setCpu(new Random().nextInt(32000) + "m");
    containerProfile.setMemory(new Random().nextInt(32) + "Gi");
    profile.getSpec().getContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);
    profile.getSpec().getInitContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);

    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getProfile()).thenReturn(profile);
  }

  @Override
  protected boolean filterContainers(Container container) {
    return !Objects.equals(
        container.getName(), StackGresContainer.PATRONI.getNameWithPrefix());
  }

  @Override
  protected Map<String, StackGresProfileContainer> getFilteredContainerResource() {
    return Map.of(
        StackGresContainer.PATRONI.getNameWithPrefix(),
        new StackGresProfileContainerBuilder()
        .withCpu(getProfile().getSpec().getCpu())
        .withMemory(getProfile().getSpec().getMemory())
        .withHugePages(getProfile().getSpec().getHugePages())
        .build());
  }

  @Override
  protected Map<String, StackGresProfileContainer> getFilteredContainerResourceRequests() {
    return Map.of(
        StackGresContainer.PATRONI.getNameWithPrefix(),
        new StackGresProfileContainerBuilder()
        .withCpu(Optional.of(getProfile().getSpec().getRequests().getCpu())
            .map(Quantity::new)
            .map(Quantity::getNumericalAmount)
            .map(amount -> statefulSet.getSpec().getTemplate().getSpec().getContainers()
                .stream()
                .filter(container -> !container.getName()
                    .equals(StackGresContainer.PATRONI.getNameWithPrefix()))
                .map(Container::getResources)
                .map(ResourceRequirements::getRequests)
                .map(requests -> requests.get("cpu"))
                .map(Quantity::getNumericalAmount)
                .reduce(
                    amount,
                    (calculated, container) -> calculated.subtract(container),
                    (u, v) -> v))
            .map(ResourceUtil::toCpuValue)
            .get())
        .withMemory(Optional.of(getProfile().getSpec().getRequests().getMemory())
            .map(Quantity::new)
            .map(Quantity::getNumericalAmount)
            .map(amount -> statefulSet.getSpec().getTemplate().getSpec().getContainers()
                .stream()
                .filter(container -> !container.getName()
                    .equals(StackGresContainer.PATRONI.getNameWithPrefix()))
                .map(Container::getResources)
                .map(ResourceRequirements::getRequests)
                .map(requests -> requests.get("memory"))
                .map(Quantity::getNumericalAmount)
                .reduce(
                    amount,
                    (calculated, container) -> calculated.subtract(container),
                    (u, v) -> v))
            .map(ResourceUtil::toMemoryValue)
            .get())
        .build());
  }

  @Override
  protected StackGresProfile getProfile() {
    return profile;
  }

  @Override
  protected PodSpec getPodSpec() {
    return statefulSet.getSpec().getTemplate().getSpec();
  }

  @Override
  protected StackGresGroupKind getKind() {
    return KIND;
  }

  @Override
  protected void decorate() {
    resources.forEach(resource -> profileDecorator.decorate(context, resource));
  }

  @Override
  protected void disableResourceRequirements() {
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setDisableClusterResourceRequirements(true);
    when(context.calculateDisablePatroniResourceRequirements()).thenReturn(true);
    when(context.calculateDisableClusterResourceRequirements()).thenReturn(true);
  }

  @Override
  protected void enableLimits() {
    cluster.getSpec().getPods().setResources(new StackGresClusterResources());
    cluster.getSpec().getPods().getResources().setEnableClusterLimitsRequirements(true);
  }

}
