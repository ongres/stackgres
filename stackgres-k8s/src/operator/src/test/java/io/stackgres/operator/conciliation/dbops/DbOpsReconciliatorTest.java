/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsReconciliatorTest {

  private final StackGresDbOps dbOps = Fixtures.dbOps().loadRestart().get();
  @Mock
  CustomResourceFinder<StackGresDbOps> finder;
  @Mock
  AbstractConciliator<StackGresDbOps> conciliator;
  @Mock
  DeployedResourcesCache deployedResourcesCache;
  @Mock
  HandlerDelegator<StackGresDbOps> handlerDelegator;
  @Mock
  DbOpsStatusManager statusManager;
  @Mock
  EventEmitter<StackGresDbOps> eventController;
  @Mock
  CustomResourceScheduler<StackGresDbOps> dbOpsScheduler;
  @Mock
  Metrics metrics;

  private DbOpsReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    DbOpsReconciliator.Parameters parameters = new DbOpsReconciliator.Parameters();
    parameters.finder = finder;
    parameters.conciliator = conciliator;
    parameters.deployedResourcesCache = deployedResourcesCache;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.dbOpsScheduler = dbOpsScheduler;
    parameters.objectMapper = JsonUtil.jsonMapper();
    parameters.metrics = metrics;
    reconciliator = new DbOpsReconciliator(parameters);
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

}
