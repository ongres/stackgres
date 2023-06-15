/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.conciliation.Conciliator;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsReconciliatorTest {

  private final StackGresDistributedLogs distributedlogs =
      Fixtures.distributedLogs().loadDefault().get();
  @Mock
  Conciliator<StackGresDistributedLogs> conciliator;
  @Mock
  HandlerDelegator<StackGresDistributedLogs> handlerDelegator;
  @Mock
  StatusManager<StackGresDistributedLogs, Condition> statusManager;
  @Mock
  EventEmitter<StackGresDistributedLogs> eventController;
  @Mock
  CustomResourceScheduler<StackGresDistributedLogs> distributedlogsScheduler;
  @Mock
  ConnectedClustersScanner connectedClustersScanner;

  private DistributedLogsReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    final DistributedLogsReconciliator.Parameters parameters =
        new DistributedLogsReconciliator.Parameters();
    parameters.conciliator = conciliator;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.distributedLogsScheduler = distributedlogsScheduler;
    parameters.connectedClustersScanner = connectedClustersScanner;
    reconciliator = new DistributedLogsReconciliator(parameters);
  }

  @Test
  void allCreations_shouldBePerformed() {
    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(distributedlogs, resource))
        .thenReturn(resource));

    when(conciliator.evalReconciliationState(distributedlogs))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(List.of(distributedlogs));

    verify(conciliator).evalReconciliationState(distributedlogs);
    creations.forEach(resource -> verify(handlerDelegator).create(distributedlogs, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(
        resource -> when(handlerDelegator.patch(distributedlogs, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(conciliator.evalReconciliationState(distributedlogs))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle(List.of(distributedlogs));

    verify(conciliator).evalReconciliationState(distributedlogs);
    patches.forEach(resource -> verify(handlerDelegator)
        .patch(distributedlogs, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator)
        .delete(distributedlogs, resource));

    when(conciliator.evalReconciliationState(distributedlogs))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle(List.of(distributedlogs));

    verify(conciliator).evalReconciliationState(distributedlogs);
    deletions.forEach(resource -> verify(handlerDelegator)
        .delete(distributedlogs, resource));
  }

}
