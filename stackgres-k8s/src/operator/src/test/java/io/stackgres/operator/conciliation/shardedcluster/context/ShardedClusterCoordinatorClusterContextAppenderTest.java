/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.testutil.JsonUtil;
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

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedClusterCoordinatorClusterContextAppender(
        shardedClusterCoordinatorPrimaryEndpointsContextAppender,
        clusterFinder,
        JsonUtil.jsonMapper());
  }

  @Test
  void givenCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder, Optional.empty());
    ArgumentCaptor<StackGresCluster> coordinator = ArgumentCaptor.captor();
    verify(contextBuilder).coordinator(coordinator.capture());
    verify(contextBuilder).foundCoordinatorCluster(Optional.empty());
    verify(shardedClusterCoordinatorPrimaryEndpointsContextAppender).appendContext(
        coordinator.getValue(), contextBuilder);
  }

  @Test
  void givenClusterWithCoordinatorCluster_shouldPassTheFoundCoordinatorCluster() {
    final StackGresCluster foundCoordinator = Fixtures.cluster().loadDefault().get();
    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(foundCoordinator));
    contextAppender.appendContext(cluster, contextBuilder, Optional.empty());
    verify(contextBuilder).foundCoordinatorCluster(Optional.of(foundCoordinator));
  }

}
