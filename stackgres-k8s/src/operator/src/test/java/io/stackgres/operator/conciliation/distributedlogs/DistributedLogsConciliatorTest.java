/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.ConciliatorTest;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsConciliatorTest extends ConciliatorTest<StackGresDistributedLogs> {

  private static final StackGresDistributedLogs distributedLogs = JsonUtil
      .readFromJson("distributedlogs/default.json", StackGresDistributedLogs.class);

  @Mock
  private RequiredResourceGenerator<StackGresDistributedLogs> requiredResourceGenerator;

  @Mock
  private DeployedResourcesScanner<StackGresDistributedLogs> deployedResourcesScanner;

  @Mock
  private DistributedLogsStatusManager statusManager;

  @BeforeEach
  void setUp() {
    when(statusManager.isPendingRestart(getConciliationResource())).thenReturn(false);
  }

  @Override
  protected Conciliator<StackGresDistributedLogs> buildConciliator(List<HasMetadata> required,
                                                                   List<HasMetadata> deployed) {
    when(requiredResourceGenerator.getRequiredResources(distributedLogs))
        .thenReturn(required);
    when(deployedResourcesScanner.getDeployedResources(distributedLogs))
        .thenReturn(deployed);

    final DistributedLogsConciliator clusterConciliator =
        new DistributedLogsConciliator(statusManager);
    clusterConciliator.setRequiredResourceGenerator(requiredResourceGenerator);
    clusterConciliator.setDeployedResourcesScanner(deployedResourcesScanner);
    clusterConciliator.setResourceComparator(resourceComparator);
    return clusterConciliator;
  }

  @Override
  protected StackGresDistributedLogs getConciliationResource() {
    return distributedLogs;
  }

  @Test
  @DisplayName("Conciliation Should Ignore Deletions On Resources Marked With Reconciliation "
      + "Pause Until Restart Annotation If The Cluster Is Pending To Restart")
  void shouldIgnoreDeletionsMarkedWithPauseUntilRestartAnnotationIfTheClusterIsPendingToRestart() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    final List<HasMetadata> deployedResources = new ArrayList<>(requiredResources);

    int indexToRemove = new Random().nextInt(requiredResources.size());
    deployedResources.get(indexToRemove).getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY,
        Boolean.TRUE.toString()));

    requiredResources.remove(indexToRemove);

    Conciliator<StackGresDistributedLogs> conciliator = buildConciliator(requiredResources,
        deployedResources);

    reset(statusManager);
    when(statusManager.isPendingRestart(distributedLogs))
        .thenReturn(true);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(0, result.getDeletions().size());

    assertTrue(result.isUpToDate());
  }

  @Test
  @DisplayName("Conciliation Should Not Ignore Deletions On Resources Marked With Reconciliation "
      + "Pause Until Restart Annotation If The Cluster Is Not Pending To Restart")
  void shouldNotIgnoreDeletionsMarkedPauseUntilRestartAnnotationClusterIsNotPendingToRestart() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    final List<HasMetadata> deployedResources = new ArrayList<>(requiredResources);

    int indexToRemove = new Random().nextInt(requiredResources.size());
    deployedResources.get(indexToRemove).getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY,
        Boolean.TRUE.toString()));

    requiredResources.remove(indexToRemove);

    Conciliator<StackGresDistributedLogs> conciliator = buildConciliator(requiredResources,
        deployedResources);

    when(statusManager.isPendingRestart(distributedLogs))
        .thenReturn(false);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(1, result.getDeletions().size());

    assertFalse(result.isUpToDate());
  }

  @Test
  @DisplayName("Conciliation Should Ignore Changes On Resources Marked With Reconciliation "
      + "Pause Until Restart Annotation If The Cluster Is Pending To Restart")
  void shouldIgnoreChangesMarkedPauseUntilRestartAnnotationIfClusterIsPendingToRestart() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");
    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    deployedResources.stream().findAny()
        .orElseThrow().getMetadata().setAnnotations(Map.of(
            StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY, Boolean.TRUE.toString()));

    Conciliator<StackGresDistributedLogs> conciliator =
        buildConciliator(requiredResources, deployedResources);

    reset(statusManager);
    when(statusManager.isPendingRestart(distributedLogs))
        .thenReturn(true);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());

    assertEquals(0, result.getPatches().size());

    assertTrue(result.isUpToDate());
  }

  @Test
  @DisplayName("Conciliation Should Not Ignore Changes On Resources Marked With Reconciliation "
      + "Pause Until Restart Annotation If The Cluster Is Not Pending To Restart")
  void shouldNotIgnoreChangesMarkedPauseUntilRestartAnnotationIfClusterIsNotPendingToRestart() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");
    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    deployedResources.stream().findAny()
        .orElseThrow().getMetadata().setAnnotations(Map.of(
            StackGresContext.RECONCILIATION_PAUSE_UNTIL_RESTART_KEY, Boolean.TRUE.toString()));

    Conciliator<StackGresDistributedLogs> conciliator =
        buildConciliator(requiredResources, deployedResources);

    when(statusManager.isPendingRestart(distributedLogs))
        .thenReturn(false);

    ReconciliationResult result = conciliator.evalReconciliationState(distributedLogs);

    assertEquals(1, result.getPatches().size());

    assertFalse(result.isUpToDate());
  }
}
