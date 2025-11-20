/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil.buildResources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LoadBalancerIngressBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.ServiceStatusBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatusBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniCtlInstance;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.DeployedResourcesSnapshot;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceKey;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterConciliatorTest {

  private StackGresCluster cluster;

  @Mock
  private CustomResourceFinder<StackGresCluster> finder;

  @Mock
  private RequiredResourceGenerator<StackGresCluster> requiredResourceGenerator;

  @Mock
  private AbstractDeployedResourcesScanner<StackGresCluster> deployedResourcesScanner;

  @Mock
  private PatroniCtl patroniCtl;

  @Mock
  private PatroniCtlInstance patroniCtlInstance;

  private DeployedResourcesCache deployedResourcesCache;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().setInstances(2);
    lenient().when(patroniCtl.instanceFor(any())).thenReturn(patroniCtlInstance);
  }

  @Test
  void nonDeployedResources_shouldAppearInTheCreation() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources =
        new ArrayList<>(deepCopy(requiredResources));

    final List<HasMetadata> deployedResources =
        new ArrayList<>(deepCopy(lastRequiredResources));

    final List<HasMetadata> foundDeployedResources =
        new ArrayList<>(deepCopy(deployedResources));

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var deletedResource = Seq.seq(deployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    deletedResource.v1.getMetadata().setManagedFields(null);
    lastRequiredResources.remove(deletedResource.v2.intValue());
    deployedResources.remove(deletedResource.v2.intValue());
    foundDeployedResources.remove(deletedResource.v2.intValue());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(1, result.getCreations().size());
    assertEquals(deletedResource.v1, result.getCreations().get(0));

    assertFalse(result.isUpToDate());
  }

  @Test
  void resourceToDelete_shouldAppearInTheDeletions() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    var deletedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    requiredResources.remove(deletedResource.v2.intValue());

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(1, result.getDeletions().size());
    assertEquals(deletedResource.v1, result.getDeletions().get(0));

    assertFalse(result.isUpToDate());
  }

  @Test
  void whenThereIsNoChanges_allResourcesShouldBeEmpty() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));

    assertTrue(result.isUpToDate());
  }

  @Test
  void whenThereAreRequiredChanges_shouldBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(requiredResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setLabels(ImmutableMap.of(StringUtil.generateRandom(), StringUtil.generateRandom()));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v1);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataLabels_shouldBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    var updatedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    String key = StringUtil.generateRandom();
    requiredResources.get(updatedResource.v2.intValue()).getMetadata()
        .setAnnotations(ImmutableMap.of(key, StringUtil.generateRandom()));
    updatedResource.v1.getMetadata()
        .setLabels(ImmutableMap.of(key, StringUtil.generateRandom()));

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataAnnotations_shouldBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    var updatedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    String key = StringUtil.generateRandom();
    requiredResources.get(updatedResource.v2.intValue()).getMetadata()
        .setAnnotations(ImmutableMap.of(key, StringUtil.generateRandom()));
    updatedResource.v1.getMetadata()
        .setAnnotations(ImmutableMap.of(key, StringUtil.generateRandom() + "-changed"));

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource.v1, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataOwnerReferences_shouldDoNothing() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));
  }

  @Test
  void whenThereAreDeployedWithOtherMetadataOwnerReferences_shouldDoNoting() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(Predicate.not(t -> t.v1 instanceof Pod))
        .filter(t -> hasAnotherOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(updatedResource.v1)));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));
  }

  @Test
  void whenThereAreDeployedChangesOnStatefulSetSpec_shouldBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    StatefulSet updatedResource = foundDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get();
    updatedResource
        .setSpec(new StatefulSetSpecBuilder()
            .withServiceName("test")
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnServiceSpec_shouldBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    Service updatedResource = foundDeployedResources.stream()
        .filter(Service.class::isInstance)
        .map(Service.class::cast)
        .findFirst()
        .get();
    updatedResource
        .setSpec(new ServiceSpecBuilder()
            .withType("test")
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  @Test
  void whenThereAreDeployedChangesOnMetadataResourceVersion_shouldNotBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var updatedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .findFirst()
        .get();
    updatedResource.v1.getMetadata()
        .setResourceVersion("test");

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));
  }

  @Test
  void whenThereAreDeployedChangesOnStatefulSetStatus_shouldNotBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    foundDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get()
        .setStatus(new StatefulSetStatusBuilder()
            .withCurrentReplicas(3)
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));
  }

  @Test
  void whenThereAreDeployedChangesOnServiceStatus_shouldNotBeDetected() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    foundDeployedResources.stream()
        .filter(Service.class::isInstance)
        .map(Service.class::cast)
        .findFirst()
        .get()
        .setStatus(new ServiceStatusBuilder()
            .withLoadBalancer(new LoadBalancerStatusBuilder()
                .withIngress(List.of(new LoadBalancerIngressBuilder()
                    .withHostname("test")
                    .build()))
                .build())
            .build());

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));
  }

  @Test
  void conciliation_shouldDetectStatefulSetChanges() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    lastRequiredResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get().getSpec().setReplicas(10);
    StatefulSet updatedResource = foundDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get();

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  @Test
  void conciliation_shouldIgnoreChangesOnResourcesMarkedWithReconciliationPauseAnnotatinon() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    foundDeployedResources.stream().forEach(resource -> resource
        .getMetadata().setAnnotations(Map.of(
            StackGresContext.RECONCILIATION_PAUSE_KEY, Boolean.TRUE.toString())));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);

    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));

    assertTrue(result.isUpToDate());
  }

  @Test
  void conciliation_shouldIgnoreDeletionsOnResourcesMarkedWithReconciliationPauseAnnotation() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    var removedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .filter(t -> t.v2.intValue() < requiredResources.size() / 2)
        .findFirst()
        .get();
    requiredResources.remove(removedResource.v2.intValue());
    removedResource.v1.getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_KEY,
        Boolean.TRUE.toString()));

    var changedResource = Seq.seq(foundDeployedResources)
        .zipWithIndex()
        .filter(t -> hasControllerOwnerReference(t.v1))
        .sorted(shuffle())
        .filter(t -> t.v2.intValue() > removedResource.v2.intValue())
        .findFirst()
        .get();
    changedResource.v1.getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_KEY,
        Boolean.FALSE.toString()));

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);
    assertEquals(0, result.getDeletions().size(),
        result.getDeletions().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getCreations().size(),
        result.getCreations().stream().map(t -> t.getKind()).collect(Collectors.joining(", ")));
    assertEquals(0, result.getPatches().size(),
        result.getPatches().stream().map(t -> t.v2.getKind()).collect(Collectors.joining(", ")));

    assertTrue(result.isUpToDate());
  }

  @Test
  void conciliation_shouldDetectStatefulSetChangesOnMissingPrimaryPod() {
    final List<HasMetadata> requiredResources = buildResources(cluster);

    requiredResources.removeIf(Predicate.not(this::hasControllerOwnerReference));

    final List<HasMetadata> lastRequiredResources = deepCopy(requiredResources);

    final List<HasMetadata> deployedResources = deepCopy(lastRequiredResources);

    final List<HasMetadata> foundDeployedResources = deepCopy(deployedResources);

    StatefulSet updatedResource = foundDeployedResources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .findFirst()
        .get();

    ClusterConciliator conciliator = buildConciliator(
        requiredResources,
        lastRequiredResources,
        deployedResources,
        foundDeployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(cluster);

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v2);
  }

  protected List<HasMetadata> deepCopy(List<HasMetadata> source) {
    return source.stream()
        .map(JsonUtil::copy)
        .toList();
  }

  protected ClusterConciliator buildConciliator(
      List<HasMetadata> required,
      List<HasMetadata> lastRequired,
      List<HasMetadata> deployed,
      List<HasMetadata> foundDeployed) {
    deployedResourcesCache = new DeployedResourcesCache(
        new OperatorPropertyContext(), JsonUtil.jsonMapper());
    required.forEach(resource -> resource.getMetadata().setManagedFields(null));
    deployed.stream()
        .filter(this::hasControllerOwnerReference)
        .forEach(resource -> deployedResourcesCache
            .put(cluster, lastRequired.stream()
                .filter(r -> ResourceKey.same(cluster, r, resource))
                .findFirst()
                .orElse(resource),
                resource));
    List<HasMetadata> ownedLastDeployed = foundDeployed.stream()
        .filter(this::hasControllerOwnerReference)
        .toList();
    foundDeployed.stream()
        .forEach(resource -> resource.getMetadata().setResourceVersion("changed"));
    DeployedResourcesSnapshot deplyedResourcesSnapshot =
        deployedResourcesCache.createDeployedResourcesSnapshot(cluster, ownedLastDeployed, foundDeployed);

    when(finder.findByNameAndNamespace(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    when(requiredResourceGenerator.getRequiredResources(cluster))
        .thenReturn(required);
    when(deployedResourcesScanner.getDeployedResources(cluster, required))
        .thenReturn(deplyedResourcesSnapshot);

    final ClusterConciliator clusterConciliator = new ClusterConciliator(
        null,
        finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache,
        new ClusterLabelFactory(new ClusterLabelMapper()),
        patroniCtl);
    return clusterConciliator;
  }

  private boolean hasAnotherOwnerReference(HasMetadata resource) {
    return resource.getMetadata().getOwnerReferences() == null
        || resource.getMetadata().getOwnerReferences().isEmpty()
        || resource.getMetadata().getOwnerReferences().stream()
        .noneMatch(ownerReference -> ownerReference.getKind()
            .equals(HasMetadata.getKind(cluster.getClass()))
            && ownerReference.getApiVersion().equals(HasMetadata.getApiVersion(cluster.getClass()))
            && ownerReference.getName().equals(cluster.getMetadata().getName())
            && ownerReference.getUid().equals(cluster.getMetadata().getUid())
            && ownerReference.getController() != null
            && ownerReference.getController());
  }

  private boolean hasControllerOwnerReference(HasMetadata resource) {
    return resource.getMetadata().getOwnerReferences() != null
        && resource.getMetadata().getOwnerReferences().stream()
        .anyMatch(ownerReference -> ownerReference.getKind()
            .equals(HasMetadata.getKind(cluster.getClass()))
            && ownerReference.getApiVersion().equals(HasMetadata.getApiVersion(cluster.getClass()))
            && ownerReference.getName().equals(cluster.getMetadata().getName())
            && ownerReference.getUid().equals(cluster.getMetadata().getUid())
            && ownerReference.getController() != null
            && ownerReference.getController());
  }

  public static <T> Comparator<T> shuffle() {
    Random random = new Random();
    Map<T, Integer> uniqueIds = new HashMap<>();
    return Comparator.comparing(e -> uniqueIds.computeIfAbsent(e, k -> random.nextInt()));
  }

}
