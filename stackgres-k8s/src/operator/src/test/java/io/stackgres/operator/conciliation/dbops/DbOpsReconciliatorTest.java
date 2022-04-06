/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

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
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsCondition;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.cluster.factory.KubernetessMockResourceGenerationUtil;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
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
class DbOpsReconciliatorTest {

  private final StackGresDbOps dbOps = JsonUtil
      .readFromJson("stackgres_dbops/dbops_restart.json", StackGresDbOps.class);
  @Mock
  CustomResourceScanner<StackGresDbOps> dbOpsScanner;
  @Mock
  Conciliator<StackGresDbOps> dbOpsConciliator;
  @Mock
  HandlerDelegator<StackGresDbOps> handlerDelegator;
  @Mock
  StatusManager<StackGresDbOps, StackGresDbOpsCondition> statusManager;
  @Mock
  EventEmitter<StackGresDbOps> eventController;
  @Mock
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;
  @Mock
  ComparisonDelegator<StackGresDbOps> resourceComparator;

  private DbOpsReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = new DbOpsReconciliator();
    reconciliator.setScanner(dbOpsScanner);
    reconciliator.setConciliator(dbOpsConciliator);
    reconciliator.setHandlerDelegator(handlerDelegator);
    reconciliator.setEventController(eventController);
    reconciliator.setStatusManager(statusManager);
    reconciliator.setDbOpsScheduler(dbOpsScheduler);
    reconciliator.setResourceComparator(resourceComparator);
  }

  @Test
  void allCreations_shouldBePerformed() {
    when(dbOpsScanner.getResources()).thenReturn(Collections.singletonList(dbOps));

    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(dbOps, resource))
        .thenReturn(resource));

    when(dbOpsConciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(dbOpsScanner).getResources();
    verify(dbOpsConciliator).evalReconciliationState(dbOps);
    creations.forEach(resource -> verify(handlerDelegator).create(dbOps, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    when(dbOpsScanner.getResources()).thenReturn(Collections.singletonList(dbOps));

    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(dbOps, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(dbOpsConciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(dbOpsScanner).getResources();
    verify(dbOpsConciliator).evalReconciliationState(dbOps);
    patches.forEach(resource -> verify(handlerDelegator).patch(dbOps, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    when(dbOpsScanner.getResources()).thenReturn(Collections.singletonList(dbOps));

    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(dbOps, resource));

    when(dbOpsConciliator.evalReconciliationState(dbOps))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle();

    verify(dbOpsScanner).getResources();
    verify(dbOpsConciliator).evalReconciliationState(dbOps);
    deletions.forEach(resource -> verify(handlerDelegator).delete(dbOps, resource));
  }

  @Test
  void reconciliator_shouldPreventTheConcurrentExecution() throws InterruptedException {

    long delay = 100;
    int concurrentExecutions = new Random().nextInt(2) + 2;

    doAnswer(new AnswersWithDelay(delay, new Returns(Collections.singletonList(dbOps))))
        .when(dbOpsScanner).getResources();

    when(dbOpsConciliator.evalReconciliationState(dbOps))
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

    verify(dbOpsScanner, times(concurrentExecutions)).getResources();
    verify(dbOpsConciliator, times(concurrentExecutions)).evalReconciliationState(dbOps);

  }
}
