/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsClusterContextAppenderTest {

  private DistributedLogsClusterContextAppender contextAppender;

  private StackGresDistributedLogs distributedLogs;

  private StackGresCluster cluster;

  @Spy
  private StackGresDistributedLogsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  @BeforeEach
  void setUp() {
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new DistributedLogsClusterContextAppender(clusterFinder);
  }

  @Test
  void givenDistributedLogsWithCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        distributedLogs.getMetadata().getName(),
        distributedLogs.getMetadata().getNamespace()))
        .thenReturn(Optional.of(cluster));
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).cluster(Optional.of(cluster));
  }

  @Test
  void givenDistributedLogsWithoutCluster_shouldPass() {
    when(clusterFinder.findByNameAndNamespace(
        distributedLogs.getMetadata().getName(),
        distributedLogs.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).cluster(Optional.empty());
  }

}
