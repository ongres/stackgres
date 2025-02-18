/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatusBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsClusterContextAppenderTest {

  private DbOpsClusterContextAppender contextAppender;

  private StackGresDbOps dbOps;

  private StackGresCluster cluster;

  @Spy
  private StackGresDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @Mock
  private DbOpsClusterInstanceProfileContextAppender dbOpsClusterInstanceProfileContextAppender;

  @Mock
  private DbOpsClusterMajorVersionUpgradeContextAppender dbOpsClusterMajorVersionUpgradeContextAppender;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.dbOps().loadPgbench().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new DbOpsClusterContextAppender(
        clusterFinder,
        dbOpsClusterInstanceProfileContextAppender,
        dbOpsClusterMajorVersionUpgradeContextAppender);
  }

  @Test
  void givenDbOpsWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        dbOps.getSpec().getSgCluster(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.of(cluster));
    verify(dbOpsClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(dbOpsClusterMajorVersionUpgradeContextAppender, Mockito.never())
        .appendContext(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void givenDbOpsWithoutCluster_shouldFail() {
    when(clusterFinder.findByNameAndNamespace(
        dbOps.getSpec().getSgCluster(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGCluster stackgres was not found", ex.getMessage());
  }

  @Test
  void givenCompletedDbOpsWithoutCluster_shouldPass() {
    dbOps.setStatus(
        new StackGresDbOpsStatusBuilder()
        .withConditions(DbOpsStatusCondition.DBOPS_COMPLETED.getCondition())
        .build());
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.empty());
    verify(clusterFinder, Mockito.never()).findByNameAndNamespace(
        dbOps.getSpec().getSgCluster(),
        dbOps.getMetadata().getNamespace());
    verify(dbOpsClusterInstanceProfileContextAppender, Mockito.never()).appendContext(Mockito.any(), Mockito.any());
    verify(dbOpsClusterMajorVersionUpgradeContextAppender, Mockito.never())
        .appendContext(Mockito.any(), Mockito.any(), Mockito.any());
  }

  @Test
  void givenValidMajorVersionUpgradeDbOps_shouldPass() {
    dbOps = Fixtures.dbOps().loadMajorVersionUpgrade().get();
    when(clusterFinder.findByNameAndNamespace(
        dbOps.getSpec().getSgCluster(),
        dbOps.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).foundCluster(Optional.of(cluster));
    verify(dbOpsClusterInstanceProfileContextAppender).appendContext(cluster, contextBuilder);
    verify(dbOpsClusterMajorVersionUpgradeContextAppender).appendContext(dbOps, cluster, contextBuilder);
  }

}
