/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.ComparisonDelegator;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupReconciliatorTest {

  private final StackGresBackup backup = Fixtures.backup().loadDefault().get();
  @Mock
  Conciliator<StackGresBackup> conciliator;
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
    BackupReconciliator.Parameters parameters = new BackupReconciliator.Parameters();
    parameters.backupScheduler = backupScheduler;
    parameters.conciliator = conciliator;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.backupScheduler = backupScheduler;
    parameters.resourceComparator = resourceComparator;
    parameters.statusManager = statusManager;
    reconciliator = spy(new BackupReconciliator(parameters));
  }

  @Test
  void allCreations_shouldBePerformed() {
    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(backup, resource))
        .thenReturn(resource));

    when(conciliator.evalReconciliationState(backup))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(List.of(backup));

    verify(conciliator).evalReconciliationState(backup);
    creations.forEach(resource -> verify(handlerDelegator).create(backup, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(backup, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(conciliator.evalReconciliationState(backup))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle(List.of(backup));

    verify(conciliator).evalReconciliationState(backup);
    patches.forEach(resource -> verify(handlerDelegator).patch(backup, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(backup, resource));

    when(conciliator.evalReconciliationState(backup))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle(List.of(backup));

    verify(conciliator).evalReconciliationState(backup);
    deletions.forEach(resource -> verify(handlerDelegator).delete(backup, resource));
  }

}
