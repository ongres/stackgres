/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
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
class ConfigReconciliatorTest {

  private final StackGresConfig config =
      Fixtures.config().loadDefault().get();
  @Mock
  CustomResourceFinder<StackGresConfig> finder;
  @Mock
  AbstractConciliator<StackGresConfig> conciliator;
  @Mock
  DeployedResourcesCache deployedResourcesCache;
  @Mock
  HandlerDelegator<StackGresConfig> handlerDelegator;
  @Mock
  StatusManager<StackGresConfig, Condition> statusManager;
  @Mock
  EventEmitter<StackGresConfig> eventController;
  @Mock
  Metrics metrics;

  private ConfigReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    final ConfigReconciliator.Parameters parameters =
        new ConfigReconciliator.Parameters();
    parameters.finder = finder;
    parameters.conciliator = conciliator;
    parameters.deployedResourcesCache = deployedResourcesCache;
    parameters.handlerDelegator = handlerDelegator;
    parameters.eventController = eventController;
    parameters.statusManager = statusManager;
    parameters.metrics = metrics;
    reconciliator = new ConfigReconciliator(parameters);
  }

  @Test
  void allCreations_shouldBePerformed() {
    final List<HasMetadata> creations = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    creations.forEach(resource -> when(handlerDelegator.create(config, resource))
        .thenReturn(resource));

    when(conciliator.evalReconciliationState(config))
        .thenReturn(new ReconciliationResult(
            creations,
            Collections.emptyList(),
            Collections.emptyList()));

    reconciliator.reconciliationCycle(config, 0, false);

    verify(conciliator).evalReconciliationState(config);
    creations.forEach(resource -> verify(handlerDelegator).create(config, resource));
  }

  @Test
  void allPatches_shouldBePerformed() {
    final List<Tuple2<HasMetadata, HasMetadata>> patches = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test")
        .stream().map(r -> Tuple.tuple(r, r))
        .collect(Collectors.toUnmodifiableList());

    patches.forEach(
        resource -> when(handlerDelegator.patch(config, resource.v1, resource.v2))
        .thenReturn(resource.v1));

    when(conciliator.evalReconciliationState(config))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            patches,
            Collections.emptyList()));

    reconciliator.reconciliationCycle(config, 0, false);

    verify(conciliator).evalReconciliationState(config);
    patches.forEach(resource -> verify(handlerDelegator)
        .patch(config, resource.v1, resource.v2));
  }

  @Test
  void allDeletions_shouldBePerformed() {
    final List<HasMetadata> deletions = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    deletions.forEach(resource -> doNothing().when(handlerDelegator)
        .delete(config, resource));

    when(conciliator.evalReconciliationState(config))
        .thenReturn(new ReconciliationResult(
            Collections.emptyList(),
            Collections.emptyList(),
            deletions));

    reconciliator.reconciliationCycle(config, 0, false);

    verify(conciliator).evalReconciliationState(config);
    deletions.forEach(resource -> verify(handlerDelegator)
        .delete(config, resource));
  }

}
