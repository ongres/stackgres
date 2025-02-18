/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsStatusCondition;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsStatusBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsClusterCoordinatorContextAppenderTest {

  private ShardedDbOpsClusterCoordinatorContextAppender contextAppender;

  private StackGresShardedDbOps dbOps;

  private StackGresCluster cluster;

  @Spy
  private StackGresShardedDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ShardedDbOpsClusterCoordinatorContextAppender(
        clusterFinder);
  }

  @Test
  void givenDbOpsWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        getCoordinatorClusterName(dbOps.getSpec().getSgShardedCluster()),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundCoordinator(Optional.of(cluster));
  }

  @Test
  void givenDbOpsWithoutCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        getCoordinatorClusterName(dbOps.getSpec().getSgShardedCluster()),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGCluster test-coord was not found", ex.getMessage());
  }

  @Test
  void givenCompletedDbOpsWithoutCluster_shouldPass() {
    dbOps.setStatus(
        new StackGresShardedDbOpsStatusBuilder()
        .addToConditions(ShardedDbOpsStatusCondition.DBOPS_COMPLETED.getCondition())
        .build());
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundCoordinator(Optional.empty());
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        getCoordinatorClusterName(dbOps.getSpec().getSgShardedCluster()),
        dbOps.getMetadata().getNamespace());
  }

}
