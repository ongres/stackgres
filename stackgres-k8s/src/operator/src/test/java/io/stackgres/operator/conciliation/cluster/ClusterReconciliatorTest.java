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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
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
  CustomResourceWriter<StackGresCluster> clusterWriter;
  @Mock
  CustomResourceWriter<StackGresCluster> writer;
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
  @Mock
  OperatorPropertyContext operatorPropertyContext;
  @Mock
  MutationPipeline<StackGresCluster, StackGresClusterReview> mutationPipeline;
  @Mock
  ValidationPipeline<StackGresClusterReview> validationPipeline;

  private ClusterReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    ClusterReconciliator.Parameters parameters = new ClusterReconciliator.Parameters();
    parameters.operatorPropertyContext = operatorPropertyContext;
    parameters.mutationPipeline = mutationPipeline;
    parameters.validationPipeline = validationPipeline;
    parameters.finder = finder;
    parameters.conciliator = conciliator;
    parameters.deployedResourcesCache = deployedResourcesCache;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.clusterWriter = clusterWriter;
    parameters.writer = writer;
    parameters.labelFactory = labelFactory;
    parameters.statefulSetFinder = statefulSetFinder;
    parameters.statefulSetWriter = statefulSetWriter;
    parameters.podScanner = podScanner;
    parameters.podWriter = podWriter;
    parameters.objectMapper = JsonUtil.jsonMapper();
    parameters.metrics = metrics;
    reconciliator = new ClusterReconciliator(parameters);
    lenient()
        .when(writer.update(any(StackGresCluster.class),
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

}
