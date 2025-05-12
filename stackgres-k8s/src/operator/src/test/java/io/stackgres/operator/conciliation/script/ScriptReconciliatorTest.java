/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
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
class ScriptReconciliatorTest {

  private final StackGresScript script = Fixtures.script().loadDefault().get();
  @Mock
  AbstractConciliator<StackGresScript> conciliator;
  @Mock
  DeployedResourcesCache deployedResourcesCache;
  @Mock
  HandlerDelegator<StackGresScript> handlerDelegator;
  @Mock
  ScriptStatusManager statusManager;
  @Mock
  EventEmitter<StackGresScript> eventController;
  @Mock
  CustomResourceScheduler<StackGresScript> scriptScheduler;
  @Mock
  Metrics metrics;

  private ScriptReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    ScriptReconciliator.Parameters parameters = new ScriptReconciliator.Parameters();
    parameters.conciliator = conciliator;
    parameters.deployedResourcesCache = deployedResourcesCache;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.scriptScheduler = scriptScheduler;
    parameters.metrics = metrics;
    reconciliator = new ScriptReconciliator(parameters);
  }

  @Test
  void allCreations_shouldBePerformed() {
    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(script, resource))
        .thenReturn(resource));

    when(conciliator.evalReconciliationState(script))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(script, 0, false);

    verify(conciliator).evalReconciliationState(script);
    creations.forEach(resource -> verify(handlerDelegator).create(script, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(resource -> when(handlerDelegator.patch(script, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(conciliator.evalReconciliationState(script))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle(script, 0, false);

    verify(conciliator).evalReconciliationState(script);
    patches.forEach(resource -> verify(handlerDelegator).patch(script, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator).delete(script, resource));

    when(conciliator.evalReconciliationState(script))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle(script, 0, false);

    verify(conciliator).evalReconciliationState(script);
    deletions.forEach(resource -> verify(handlerDelegator).delete(script, resource));
  }

}
