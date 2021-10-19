/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.cluster.factory.KubernetessMockResourceGenerationUtil;
import io.stackgres.operator.conciliation.cluster.ClusterReconciliator;
import io.stackgres.testutil.JsonUtil;
import org.hamcrest.MatcherAssert;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterReconciliatorTest {

  private final StackGresCluster cluster = JsonUtil
      .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
  @Mock
  CustomResourceScanner<StackGresCluster> clusterScanner;
  @Mock
  Conciliator<StackGresCluster> clusterConciliator;
  @Mock
  HandlerDelegator<StackGresCluster> handlerDelegator;
  @Mock
  StatusManager<StackGresCluster, StackGresClusterCondition> statusManager;
  @Mock
  EventEmitter<StackGresCluster> eventController;
  @Mock
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  private ClusterReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = new ClusterReconciliator();
    reconciliator.setScanner(clusterScanner);
    reconciliator.setConciliator(clusterConciliator);
    reconciliator.setHandlerDelegator(handlerDelegator);
    reconciliator.setEventController(eventController);
    reconciliator.setStatusManager(statusManager);
    reconciliator.setClusterScheduler(clusterScheduler);
  }

  @Test
  void allCreations_shouldBePerformed() {
    when(clusterScanner.getResources()).thenReturn(Collections.singletonList(cluster));

    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(cluster, resource))
        .thenReturn(resource));

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(clusterScanner).getResources();
    verify(clusterConciliator).evalReconciliationState(cluster);
    creations.forEach(resource -> verify(handlerDelegator).create(cluster, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    when(clusterScanner.getResources()).thenReturn(Collections.singletonList(cluster));

    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(cluster, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(clusterScanner).getResources();
    verify(clusterConciliator).evalReconciliationState(cluster);
    patches.forEach(resource -> verify(handlerDelegator).patch(cluster, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    when(clusterScanner.getResources()).thenReturn(Collections.singletonList(cluster));

    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(cluster, resource));

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle();

    verify(clusterScanner).getResources();
    verify(clusterConciliator).evalReconciliationState(cluster);
    deletions.forEach(resource -> verify(handlerDelegator).delete(cluster, resource));
  }

  @Test
  void reconciliator_shouldPreventTheConcurrentExecution() throws InterruptedException {

    long delay = 100;
    int concurrentExecutions = new Random().nextInt(2) + 2;

    doAnswer(new AnswersWithDelay(delay, new Returns(Collections.singletonList(cluster))))
        .when(clusterScanner).getResources();

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    var pool = ForkJoinPool.commonPool();
    long start = System.currentTimeMillis();
    for (int i = 0; i < concurrentExecutions; i++) {
      pool.execute(() -> reconciliator.reconciliationCycle());
    }

    pool.awaitTermination(delay * concurrentExecutions, TimeUnit.SECONDS);
    long end = System.currentTimeMillis();

    MatcherAssert
        .assertThat("Is being executed concurrently",
            end - start,
            greaterThanOrEqualTo(delay * concurrentExecutions));

    verify(clusterScanner, times(concurrentExecutions)).getResources();
    verify(clusterConciliator, times(concurrentExecutions)).evalReconciliationState(cluster);

  }
}
