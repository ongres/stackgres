/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

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
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.cluster.factory.KubernetessMockResourceGenerationUtil;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
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
class BackupReconciliatorTest {

  private final StackGresBackup backup = JsonUtil
      .readFromJson("backup/default.json", StackGresBackup.class);
  @Mock
  CustomResourceScanner<StackGresBackup> backupScanner;
  @Mock
  Conciliator<StackGresBackup> backupConciliator;
  @Mock
  HandlerDelegator<StackGresBackup> handlerDelegator;
  @Mock
  EventEmitter<StackGresBackup> eventController;
  @Mock
  CustomResourceScheduler<StackGresBackup> backupScheduler;
  @Mock
  ComparisonDelegator<StackGresBackup> resourceComparator;
  @Mock
  CustomResourceFinder<StackGresCluster> clusterFinder;
  @Mock
  CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;
  @Mock
  BackupStatusManager statusManager;

  private BackupReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = new BackupReconciliator();
    reconciliator.setScanner(backupScanner);
    reconciliator.setConciliator(backupConciliator);
    reconciliator.setHandlerDelegator(handlerDelegator);
    reconciliator.setEventController(eventController);
    reconciliator.setBackupScheduler(backupScheduler);
    reconciliator.setClusterFinder(clusterFinder);
    reconciliator.setBackupConfigFinder(backupConfigFinder);
    reconciliator.setResourceComparator(resourceComparator);
  }

  @Test
  void allCreations_shouldBePerformed() {
    when(backupScanner.getResources()).thenReturn(Collections.singletonList(backup));

    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(backup, resource))
        .thenReturn(resource));

    when(backupConciliator.evalReconciliationState(backup))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(backupScanner).getResources();
    verify(backupConciliator).evalReconciliationState(backup);
    creations.forEach(resource -> verify(handlerDelegator).create(backup, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    when(backupScanner.getResources()).thenReturn(Collections.singletonList(backup));

    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(backup, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(backupConciliator.evalReconciliationState(backup))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle();

    verify(backupScanner).getResources();
    verify(backupConciliator).evalReconciliationState(backup);
    patches.forEach(resource -> verify(handlerDelegator).patch(backup, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    when(backupScanner.getResources()).thenReturn(Collections.singletonList(backup));

    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(backup, resource));

    when(backupConciliator.evalReconciliationState(backup))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle();

    verify(backupScanner).getResources();
    verify(backupConciliator).evalReconciliationState(backup);
    deletions.forEach(resource -> verify(handlerDelegator).delete(backup, resource));
  }

  @Test
  void reconciliator_shouldPreventTheConcurrentExecution() throws InterruptedException {

    long delay = 100;
    int concurrentExecutions = new Random().nextInt(2) + 2;

    doAnswer(new AnswersWithDelay(delay, new Returns(Collections.singletonList(backup))))
        .when(backupScanner).getResources();

    when(backupConciliator.evalReconciliationState(backup))
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

    verify(backupScanner, times(concurrentExecutions)).getResources();
    verify(backupConciliator, times(concurrentExecutions)).evalReconciliationState(backup);

  }
}
