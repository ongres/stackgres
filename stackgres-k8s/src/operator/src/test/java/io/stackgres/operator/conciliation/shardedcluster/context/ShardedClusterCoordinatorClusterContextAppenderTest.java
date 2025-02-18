/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.mockito.Mockito.verify;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterCoordinatorClusterContextAppenderTest {

  private ShardedClusterCoordinatorClusterContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private ShardedClusterCoordinatorPrimaryEndpointsContextAppender
      shardedClusterCoordinatorPrimaryEndpointsContextAppender;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterCoordinatorClusterContextAppender(
        shardedClusterCoordinatorPrimaryEndpointsContextAppender);
  }

  @Test
  void givenCluster_shouldPass() {
    contextAppender.appendContext(cluster, contextBuilder);
    ArgumentCaptor<StackGresCluster> coordinator = ArgumentCaptor.captor();
    verify(contextBuilder).coordinator(coordinator.capture());
    verify(shardedClusterCoordinatorPrimaryEndpointsContextAppender).appendContext(
        coordinator.getValue(), contextBuilder);
  }

}
