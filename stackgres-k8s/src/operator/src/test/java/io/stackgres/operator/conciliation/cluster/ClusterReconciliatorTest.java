/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterReconciliatorTest {

  private final StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
  @Mock
  CustomResourceFinder<StackGresCluster> finder;
  @Mock
  AbstractConciliator<StackGresCluster> conciliator;
  @Mock
  DeployedResourcesCache deployedResourcesCache;
  @Mock
  HandlerDelegator<StackGresCluster> handlerDelegator;
  @Mock
  StatusManager<StackGresCluster, Condition> statusManager;
  @Mock
  EventEmitter<StackGresCluster> eventController;
  @Mock
  CustomResourceScheduler<StackGresCluster> clusterScheduler;
  @Mock
  LabelFactoryForCluster labelFactory;
  @Mock
  ResourceFinder<StatefulSet> statefulSetFinder;
  @Mock
  ResourceWriter<StatefulSet> statefulSetWriter;
  @Mock
  ResourceScanner<Pod> podScanner;
  @Mock
  ResourceWriter<Pod> podWriter;
  @Mock
  Metrics metrics;

  private ClusterReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    ClusterReconciliator.Parameters parameters = new ClusterReconciliator.Parameters();
    parameters.finder = finder;
    parameters.conciliator = conciliator;
    parameters.deployedResourcesCache = deployedResourcesCache;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.clusterScheduler = clusterScheduler;
    parameters.labelFactory = labelFactory;
    parameters.statefulSetFinder = statefulSetFinder;
    parameters.statefulSetWriter = statefulSetWriter;
    parameters.podScanner = podScanner;
    parameters.podWriter = podWriter;
    parameters.objectMapper = JsonUtil.jsonMapper();
    parameters.metrics = metrics;
    reconciliator = new ClusterReconciliator(parameters);
    lenient()
        .when(clusterScheduler.update(any(StackGresCluster.class),
            ArgumentMatchers.<Consumer<StackGresCluster>>any()))
        .thenAnswer(invocation -> {
          final Consumer<StackGresCluster> setter = invocation.getArgument(1);
          setter.accept(cluster);
          return cluster;
        });
  }

  @Test
  void allCreations_shouldBePerformed() {
    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(cluster, resource))
        .thenReturn(resource));

    when(conciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(conciliator).evalReconciliationState(cluster);
    creations.forEach(resource -> verify(handlerDelegator).create(cluster, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(cluster, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(conciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(conciliator).evalReconciliationState(cluster);
    patches.forEach(resource -> verify(handlerDelegator).patch(cluster, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(cluster, resource));

    when(conciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(conciliator).evalReconciliationState(cluster);
    deletions.forEach(resource -> verify(handlerDelegator).delete(cluster, resource));
  }

  @Test
  void clusterWithoutFinalizer_shouldHaveTheFinalizerAdded() {
    when(conciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(cluster, 0, false);

    assertTrue(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void clusterBeingDeletedWithoutPods_shouldNotBeReconciledAndHaveTheFinalizerRemoved() {
    cluster.getMetadata().setFinalizers(
        new ArrayList<>(List.of(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER)));
    cluster.getMetadata().setDeletionTimestamp("2026-01-01T00:00:00Z");
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    when(labelFactory.clusterLabels(cluster)).thenReturn(Map.of());
    when(statefulSetFinder.findByNameAndNamespace(name, namespace))
        .thenReturn(Optional.empty());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of());

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(conciliator, never()).evalReconciliationState(cluster);
    assertFalse(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void clusterBeingDeletedOrphaningDependents_shouldNotTouchPodsAndHaveTheFinalizerRemoved() {
    setBeingDeleted(ClusterReconciliator.ORPHAN_FINALIZER);

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(statefulSetFinder, never()).findByNameAndNamespace(any(), any());
    verify(statefulSetWriter, never()).update(any());
    verify(podWriter, never()).delete(any());
    assertFalse(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void clusterBeingDeletedWithOrphanedStatefulSet_shouldNotTouchPodsAndHaveTheFinalizerRemoved() {
    setBeingDeleted();
    when(statefulSetFinder.findByNameAndNamespace(
        cluster.getMetadata().getName(), cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(statefulSet(1)));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(statefulSetWriter, never()).update(any());
    verify(podScanner, never()).getResourcesInNamespaceWithLabels(any(), any());
    verify(podWriter, never()).delete(any());
    assertFalse(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void clusterBeingDeletedWithPods_shouldScaleDownDeleteItsPodsAndKeepTheFinalizer() {
    setBeingDeleted();
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    final StatefulSet statefulSet = statefulSet(2, clusterOwnerReference());
    when(statefulSetFinder.findByNameAndNamespace(name, namespace))
        .thenReturn(Optional.of(statefulSet));
    final Pod statefulSetPod = pod(name + "-0", null, statefulSetOwnerReference());
    final Pod releasedPod = pod(name + "-1", null);
    final Pod otherPod = pod(name + "-job", null, new OwnerReferenceBuilder()
        .withKind("Job")
        .withName(name + "-job")
        .withUid("other")
        .build());
    when(labelFactory.clusterLabels(cluster)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of(statefulSetPod, releasedPod, otherPod));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(statefulSetWriter).update(ArgumentMatchers.<StatefulSet>argThat(
        updated -> updated.getSpec().getReplicas() == 0));
    verify(podWriter).delete(statefulSetPod);
    verify(podWriter).delete(releasedPod);
    verify(podWriter, never()).delete(otherPod);
    assertTrue(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void clusterBeingDeletedWithTerminatingPods_shouldKeepTheFinalizer() {
    setBeingDeleted();
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    reconciliator.clock = Clock.fixed(Instant.parse("2026-01-01T00:05:00Z"), ZoneOffset.UTC);
    when(statefulSetFinder.findByNameAndNamespace(name, namespace))
        .thenReturn(Optional.of(statefulSet(0, clusterOwnerReference())));
    when(labelFactory.clusterLabels(cluster)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of(
            pod(name + "-0", "2026-01-01T00:00:00Z", statefulSetOwnerReference()),
            pod(name + "-1", "2026-01-01T00:04:00Z", statefulSetOwnerReference())));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(podWriter, never()).delete(any());
    verify(eventController, never()).sendEvent(
        ArgumentMatchers.eq(ClusterEventReason.CLUSTER_PODS_TERMINATION_TIMEOUT), any(), any());
    assertTrue(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void clusterBeingDeletedWithPodsStuckTerminating_shouldHaveTheFinalizerRemoved() {
    setBeingDeleted();
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    reconciliator.clock = Clock.fixed(Instant.parse("2026-01-01T00:05:00Z"), ZoneOffset.UTC);
    when(statefulSetFinder.findByNameAndNamespace(name, namespace))
        .thenReturn(Optional.of(statefulSet(0, clusterOwnerReference())));
    when(labelFactory.clusterLabels(cluster)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of(
            pod(name + "-0", "2026-01-01T00:00:00Z", statefulSetOwnerReference())));

    reconciliator.reconciliationCycle(cluster, 0, false);

    verify(podWriter, never()).delete(any());
    verify(eventController).sendEvent(
        ArgumentMatchers.eq(ClusterEventReason.CLUSTER_PODS_TERMINATION_TIMEOUT), any(),
        ArgumentMatchers.eq(cluster));
    assertFalse(cluster.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  private void setBeingDeleted(String... otherFinalizers) {
    cluster.getMetadata().setUid("cluster-uid");
    cluster.getMetadata().setFinalizers(new ArrayList<>(List.of(otherFinalizers)));
    cluster.getMetadata().getFinalizers().add(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER);
    cluster.getMetadata().setDeletionTimestamp("2026-01-01T00:00:00Z");
  }

  private OwnerReference clusterOwnerReference() {
    return new OwnerReferenceBuilder()
        .withKind(StackGresCluster.KIND)
        .withName(cluster.getMetadata().getName())
        .withUid(cluster.getMetadata().getUid())
        .build();
  }

  private OwnerReference statefulSetOwnerReference() {
    return new OwnerReferenceBuilder()
        .withKind("StatefulSet")
        .withName(cluster.getMetadata().getName())
        .withUid("statefulset-uid")
        .build();
  }

  private StatefulSet statefulSet(int replicas, OwnerReference... ownerReferences) {
    return new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getMetadata().getName())
        .withOwnerReferences(ownerReferences)
        .endMetadata()
        .withNewSpec()
        .withReplicas(replicas)
        .endSpec()
        .build();
  }

  private Pod pod(String name, String deletionTimestamp, OwnerReference... ownerReferences) {
    return new PodBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name)
        .withDeletionTimestamp(deletionTimestamp)
        .withOwnerReferences(ownerReferences)
        .endMetadata()
        .build();
  }

}
