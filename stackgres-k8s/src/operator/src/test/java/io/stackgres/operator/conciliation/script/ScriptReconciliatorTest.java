/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

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
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
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
class ScriptReconciliatorTest {

  private final StackGresScript script = Fixtures.script().loadDefault().get();
  @Mock
  CustomResourceScanner<StackGresScript> scriptScanner;
  @Mock
  Conciliator<StackGresScript> scriptConciliator;
  @Mock
  HandlerDelegator<StackGresScript> handlerDelegator;
  @Mock
  ScriptStatusManager statusManager;
  @Mock
  EventEmitter<StackGresScript> eventController;
  @Mock
  CustomResourceScheduler<StackGresScript> scriptScheduler;
  @Mock
  ComparisonDelegator<StackGresScript> resourceComparator;

  private ScriptReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = new ScriptReconciliator();
    reconciliator.setScanner(scriptScanner);
    reconciliator.setConciliator(scriptConciliator);
    reconciliator.setHandlerDelegator(handlerDelegator);
    reconciliator.setEventController(eventController);
    reconciliator.setStatusManager(statusManager);
    reconciliator.setScriptScheduler(scriptScheduler);
  }

  @Test
  void allCreations_shouldBePerformed() {
    when(scriptScanner.getResources()).thenReturn(Collections.singletonList(script));

    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(script, resource))
        .thenReturn(resource));

    when(scriptConciliator.evalReconciliationState(script))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(scriptScanner).getResources();
    verify(scriptConciliator).evalReconciliationState(script);
    creations.forEach(resource -> verify(handlerDelegator).create(script, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    when(scriptScanner.getResources()).thenReturn(Collections.singletonList(script));

    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(script, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(scriptConciliator.evalReconciliationState(script))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(scriptScanner).getResources();
    verify(scriptConciliator).evalReconciliationState(script);
    patches.forEach(resource -> verify(handlerDelegator).patch(script, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    when(scriptScanner.getResources()).thenReturn(Collections.singletonList(script));

    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(script, resource));

    when(scriptConciliator.evalReconciliationState(script))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle();

    verify(scriptScanner).getResources();
    verify(scriptConciliator).evalReconciliationState(script);
    deletions.forEach(resource -> verify(handlerDelegator).delete(script, resource));
  }

  @Test
  void reconciliator_shouldPreventTheConcurrentExecution() throws InterruptedException {

    long delay = 100;
    int concurrentExecutions = new Random().nextInt(2) + 2;

    doAnswer(new AnswersWithDelay(delay, new Returns(Collections.singletonList(script))))
        .when(scriptScanner).getResources();

    when(scriptConciliator.evalReconciliationState(script))
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

    verify(scriptScanner, times(concurrentExecutions)).getResources();
    verify(scriptConciliator, times(concurrentExecutions)).evalReconciliationState(script);

  }
}
