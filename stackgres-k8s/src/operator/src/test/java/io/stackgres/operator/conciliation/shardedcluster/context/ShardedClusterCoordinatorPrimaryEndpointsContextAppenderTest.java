/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterCoordinatorPrimaryEndpointsContextAppenderTest {

  private ShardedClusterCoordinatorPrimaryEndpointsContextAppender contextAppender;

  private StackGresCluster coordinator;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Endpoints> endpointsFinder;

  @BeforeEach
  void setUp() {
    coordinator = Fixtures.cluster().loadDefault().get();
    contextAppender = new ShardedClusterCoordinatorPrimaryEndpointsContextAppender(endpointsFinder);
  }

  @Test
  void givenClusterWithEndpoints_shouldPass() {
    Endpoints endpoints =
        new EndpointsBuilder()
        .build();
    when(endpointsFinder.findByNameAndNamespace(
        PatroniUtil.readWriteName(coordinator),
        coordinator.getMetadata().getNamespace()))
        .thenReturn(Optional.of(endpoints));
    contextAppender.appendContext(coordinator, contextBuilder);
    verify(contextBuilder).coordinatorPrimaryEndpoints(Optional.of(endpoints));
  }

  @Test
  void givenClusterWithoutEndpoints_shouldPass() {
    when(endpointsFinder.findByNameAndNamespace(
        PatroniUtil.readWriteName(coordinator),
        coordinator.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(coordinator, contextBuilder);
    verify(contextBuilder).coordinatorPrimaryEndpoints(Optional.empty());
  }

}
