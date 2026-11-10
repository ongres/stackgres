/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

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
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsEventReason;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
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
class ShardedDbOpsReconciliatorTest {

  private final StackGresShardedDbOps dbOps = Fixtures.shardedDbOps().loadRestart().get();
  @Mock
  CustomResourceFinder<StackGresShardedDbOps> finder;
  @Mock
  AbstractConciliator<StackGresShardedDbOps> conciliator;
  @Mock
  DeployedResourcesCache deployedResourcesCache;
  @Mock
  HandlerDelegator<StackGresShardedDbOps> handlerDelegator;
  @Mock
  ShardedDbOpsStatusManager statusManager;
  @Mock
  EventEmitter<StackGresShardedDbOps> eventController;
  @Mock
  CustomResourceWriter<StackGresShardedDbOps> dbOpsWriter;
  @Mock
  CustomResourceWriter<StackGresShardedDbOps> writer;
  @Mock
  LabelFactoryForShardedDbOps labelFactory;
  @Mock
  ResourceFinder<Job> jobFinder;
  @Mock
  ResourceWriter<Job> jobWriter;
  @Mock
  ResourceScanner<Pod> podScanner;
  @Mock
  ResourceWriter<Pod> podWriter;
  @Mock
  Metrics metrics;
  @Mock
  OperatorPropertyContext operatorPropertyContext;
  @Mock
  MutationPipeline<StackGresShardedDbOps, StackGresShardedDbOpsReview> mutationPipeline;
  @Mock
  ValidationPipeline<StackGresShardedDbOpsReview> validationPipeline;

  private ShardedDbOpsReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    ShardedDbOpsReconciliator.Parameters parameters = new ShardedDbOpsReconciliator.Parameters();
    parameters.operatorPropertyContext = operatorPropertyContext;
    parameters.mutationPipeline = mutationPipeline;
    parameters.validationPipeline = validationPipeline;
    parameters.finder = finder;
    parameters.conciliator = conciliator;
    parameters.deployedResourcesCache = deployedResourcesCache;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.dbOpsWriter = dbOpsWriter;
    parameters.writer = writer;
    parameters.labelFactory = labelFactory;
    parameters.jobFinder = jobFinder;
    parameters.jobWriter = jobWriter;
    parameters.podScanner = podScanner;
    parameters.podWriter = podWriter;
    parameters.objectMapper = JsonUtil.jsonMapper();
    parameters.metrics = metrics;
    reconciliator = new ShardedDbOpsReconciliator(parameters);
    lenient()
        .when(writer.update(any(StackGresShardedDbOps.class),
            ArgumentMatchers.<Consumer<StackGresShardedDbOps>>any()))
        .thenAnswer(invocation -> {
          final Consumer<StackGresShardedDbOps> setter = invocation.getArgument(1);
          setter.accept(dbOps);
          return dbOps;
        });
  }

  @Test
  void allCreations_shouldBePerformed() {
    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(dbOps, resource))
        .thenReturn(resource));

    when(conciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(conciliator).evalReconciliationState(dbOps);
    creations.forEach(resource -> verify(handlerDelegator).create(dbOps, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(dbOps, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(conciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(conciliator).evalReconciliationState(dbOps);
    patches.forEach(resource -> verify(handlerDelegator).patch(dbOps, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(dbOps, resource));

    when(conciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(conciliator).evalReconciliationState(dbOps);
    deletions.forEach(resource -> verify(handlerDelegator).delete(dbOps, resource));
  }

  @Test
  void shardedDbOpsWithoutFinalizer_shouldHaveTheFinalizerAdded() {
    when(conciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    assertTrue(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void shardedDbOpsBeingDeletedWithoutPods_shouldNotBeReconciledAndHaveTheFinalizerRemoved() {
    setBeingDeleted();
    final String namespace = dbOps.getMetadata().getNamespace();
    when(jobFinder.findByNameAndNamespace(ShardedDbOpsUtil.jobName(dbOps), namespace))
        .thenReturn(Optional.empty());
    when(labelFactory.shardedDbOpsPodLabels(dbOps)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of());

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(conciliator, never()).evalReconciliationState(dbOps);
    assertFalse(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void shardedDbOpsBeingDeletedOrphaningDependents_shouldNotTouchTheJobAndRemoveTheFinalizer() {
    setBeingDeleted(ShardedDbOpsReconciliator.ORPHAN_FINALIZER);

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(jobFinder, never()).findByNameAndNamespace(any(), any());
    verify(jobWriter, never()).delete(any());
    verify(podWriter, never()).delete(any());
    assertFalse(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void shardedDbOpsBeingDeletedWithOrphanedJob_shouldNotDeleteTheJobAndRemoveTheFinalizer() {
    setBeingDeleted();
    final String namespace = dbOps.getMetadata().getNamespace();
    when(jobFinder.findByNameAndNamespace(ShardedDbOpsUtil.jobName(dbOps), namespace))
        .thenReturn(Optional.of(job(null)));
    when(labelFactory.shardedDbOpsPodLabels(dbOps)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of());

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(jobWriter, never()).delete(any());
    assertFalse(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void shardedDbOpsBeingDeletedWithPods_shouldDeleteTheJobAndItsPodsAndKeepTheFinalizer() {
    setBeingDeleted();
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    final Job job = job(null, shardedDbOpsOwnerReference());
    when(jobFinder.findByNameAndNamespace(ShardedDbOpsUtil.jobName(dbOps), namespace))
        .thenReturn(Optional.of(job));
    final Pod jobPod = pod(name + "-abcde", null);
    when(labelFactory.shardedDbOpsPodLabels(dbOps)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of(jobPod));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(jobWriter).delete(job);
    verify(podWriter).delete(jobPod);
    assertTrue(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void shardedDbOpsBeingDeletedWithTerminatingPods_shouldKeepTheFinalizer() {
    setBeingDeleted();
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    reconciliator.clock = Clock.fixed(Instant.parse("2026-01-01T00:05:00Z"), ZoneOffset.UTC);
    when(jobFinder.findByNameAndNamespace(ShardedDbOpsUtil.jobName(dbOps), namespace))
        .thenReturn(Optional.of(job("2026-01-01T00:04:00Z", shardedDbOpsOwnerReference())));
    when(labelFactory.shardedDbOpsPodLabels(dbOps)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of(
            pod(name + "-abcde", "2026-01-01T00:00:00Z"),
            pod(name + "-fghij", "2026-01-01T00:04:00Z")));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(jobWriter, never()).delete(any());
    verify(podWriter, never()).delete(any());
    verify(eventController, never()).sendEvent(
        ArgumentMatchers.eq(ShardedDbOpsEventReason.SHARDED_DBOPS_PODS_TERMINATION_TIMEOUT),
        any(), any());
    assertTrue(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  @Test
  void shardedDbOpsBeingDeletedWithPodsStuckTerminating_shouldHaveTheFinalizerRemoved() {
    setBeingDeleted();
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    reconciliator.clock = Clock.fixed(Instant.parse("2026-01-01T00:05:00Z"), ZoneOffset.UTC);
    when(jobFinder.findByNameAndNamespace(ShardedDbOpsUtil.jobName(dbOps), namespace))
        .thenReturn(Optional.of(job("2026-01-01T00:00:00Z", shardedDbOpsOwnerReference())));
    when(labelFactory.shardedDbOpsPodLabels(dbOps)).thenReturn(Map.of());
    when(podScanner.getResourcesInNamespaceWithLabels(namespace, Map.of()))
        .thenReturn(List.of(pod(name + "-abcde", "2026-01-01T00:00:00Z")));

    reconciliator.reconciliationCycle(dbOps, 0, false);

    verify(podWriter, never()).delete(any());
    verify(eventController).sendEvent(
        ArgumentMatchers.eq(ShardedDbOpsEventReason.SHARDED_DBOPS_PODS_TERMINATION_TIMEOUT), any(),
        ArgumentMatchers.eq(dbOps));
    assertFalse(dbOps.getMetadata().getFinalizers()
        .contains(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER));
  }

  private void setBeingDeleted(String... otherFinalizers) {
    dbOps.getMetadata().setUid("sharded-dbops-uid");
    dbOps.getMetadata().setFinalizers(new ArrayList<>(List.of(otherFinalizers)));
    dbOps.getMetadata().getFinalizers().add(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER);
    dbOps.getMetadata().setDeletionTimestamp("2026-01-01T00:00:00Z");
  }

  private OwnerReference shardedDbOpsOwnerReference() {
    return new OwnerReferenceBuilder()
        .withKind(StackGresShardedDbOps.KIND)
        .withName(dbOps.getMetadata().getName())
        .withUid(dbOps.getMetadata().getUid())
        .build();
  }

  private Job job(String deletionTimestamp, OwnerReference... ownerReferences) {
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(dbOps.getMetadata().getNamespace())
        .withName(ShardedDbOpsUtil.jobName(dbOps))
        .withDeletionTimestamp(deletionTimestamp)
        .withOwnerReferences(ownerReferences)
        .endMetadata()
        .build();
  }

  private Pod pod(String name, String deletionTimestamp) {
    return new PodBuilder()
        .withNewMetadata()
        .withNamespace(dbOps.getMetadata().getNamespace())
        .withName(name)
        .withDeletionTimestamp(deletionTimestamp)
        .endMetadata()
        .build();
  }

}
