/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.cluster.factory.KubernetessMockResourceGenerationUtil;
import io.stackgres.operator.conciliation.cluster.ClusterReconciliator;
import io.stackgres.common.event.EventEmitter;
import org.hamcrest.MatcherAssert;
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

  private ClusterReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = new ClusterReconciliator();
    reconciliator.setClusterScanner(clusterScanner);
    reconciliator.setClusterConciliator(clusterConciliator);
    reconciliator.setHandlerDelegator(handlerDelegator);
    reconciliator.setEventController(eventController);
    reconciliator.setStatusManager(statusManager);
  }

  @Test
  void allCreations_shouldBePerformed() {
    when(clusterScanner.getResources()).thenReturn(Collections.singletonList(cluster));

    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(resource)).thenReturn(resource));

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconcile();

    verify(clusterScanner).getResources();
    verify(clusterConciliator).evalReconciliationState(cluster);
    creations.forEach(resource -> verify(handlerDelegator).create(resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    when(clusterScanner.getResources()).thenReturn(Collections.singletonList(cluster));

    final List<HasMetadata> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    patches.forEach(resource -> when(handlerDelegator.patch(resource)).thenReturn(resource));

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconcile();

    verify(clusterScanner).getResources();
    verify(clusterConciliator).evalReconciliationState(cluster);
    patches.forEach(resource -> verify(handlerDelegator).patch(resource));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    when(clusterScanner.getResources()).thenReturn(Collections.singletonList(cluster));

    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(resource));

    when(clusterConciliator.evalReconciliationState(cluster))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconcile();

    verify(clusterScanner).getResources();
    verify(clusterConciliator).evalReconciliationState(cluster);
    deletions.forEach(resource -> verify(handlerDelegator).delete(resource));
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
      pool.execute(() -> reconciliator.reconcile());
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
