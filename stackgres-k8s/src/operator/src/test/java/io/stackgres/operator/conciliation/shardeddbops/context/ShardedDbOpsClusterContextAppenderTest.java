/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
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
class ShardedDbOpsClusterContextAppenderTest {

  private ShardedDbOpsClusterContextAppender contextAppender;

  private StackGresShardedDbOps dbOps;

  private StackGresShardedCluster cluster;

  @Spy
  private StackGresShardedDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Mock
  private ShardedDbOpsClusterInstanceProfileContextAppender dbOpsClusterInstanceProfileContextAppender;

  @Mock
  private ShardedDbOpsClusterMajorVersionUpgradeContextAppender dbOpsClusterMajorVersionUpgradeContextAppender;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    contextAppender = new ShardedDbOpsClusterContextAppender(
        clusterFinder,
        dbOpsClusterInstanceProfileContextAppender,
        dbOpsClusterMajorVersionUpgradeContextAppender);
  }

  @Test
  void givenDbOpsWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        dbOps.getSpec().getSgShardedCluster(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundShardedCluster(Optional.of(cluster));
    verify(dbOpsClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(dbOpsClusterMajorVersionUpgradeContextAppender, Mockito.never()).appendContext(
        Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void givenDbOpsWithoutCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        dbOps.getSpec().getSgShardedCluster(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGShardedCluster test was not found", ex.getMessage());
  }

  @Test
  void givenCompletedDbOpsWithoutCluster_shouldPass() {
    dbOps.setStatus(
        new StackGresShardedDbOpsStatusBuilder()
        .addToConditions(ShardedDbOpsStatusCondition.DBOPS_COMPLETED.getCondition())
        .build());
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundShardedCluster(Optional.empty());
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        dbOps.getSpec().getSgShardedCluster(),
        dbOps.getMetadata().getNamespace());
    verify(dbOpsClusterInstanceProfileContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
    verify(dbOpsClusterMajorVersionUpgradeContextAppender, Mockito.never()).appendContext(
        Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void givenMajorVersionUpgradeDbOpsWithCluster_shouldPass() {
    dbOps = Fixtures.shardedDbOps().loadMajorVersionUpgrade().get();
    when(clusterFinder.findByNameAndNamespace(
        dbOps.getSpec().getSgShardedCluster(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundShardedCluster(Optional.of(cluster));
    verify(dbOpsClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(dbOpsClusterMajorVersionUpgradeContextAppender).appendContext(dbOps, cluster, contextBuilder);
  }

}
