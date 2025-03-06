/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogsBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsConnectedClustersContextAppenderTest {

  private DistributedLogsConnectedClustersContextAppender contextAppender;

  private StackGresDistributedLogs distributedLogs;

  private StackGresCluster cluster;

  @Spy
  private StackGresDistributedLogsContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresCluster> clusterScanner;

  @BeforeEach
  void setUp() {
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new DistributedLogsConnectedClustersContextAppender(
        clusterScanner);
  }

  @Test
  void givenDistributedLogsWithDisconnectedCluster_shouldPass() {
    when(clusterScanner.getResources())
        .thenReturn(List.of(cluster));
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).connectedClusters(List.of());
    assertNotNull(distributedLogs.getStatus());
    assertNotNull(distributedLogs.getStatus().getConnectedClusters());
    assertEquals(distributedLogs.getStatus().getConnectedClusters().size(), 0);
  }

  @Test
  void givenDistributedLogsWithoutCluster_shouldFail() {
    when(clusterScanner.getResources())
        .thenReturn(List.of());
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).connectedClusters(List.of());
    assertNotNull(distributedLogs.getStatus());
    assertNotNull(distributedLogs.getStatus().getConnectedClusters());
    assertEquals(distributedLogs.getStatus().getConnectedClusters().size(), 0);
  }

  @Test
  void givenDistributedLogsWithConnectedCluster_shouldPass() {
    cluster.getSpec().setDistributedLogs(
        new StackGresClusterDistributedLogsBuilder()
        .withSgDistributedLogs(
            distributedLogs.getMetadata().getNamespace() + "." + distributedLogs.getMetadata().getName())
        .withRetention("P1M")
        .build());
    when(clusterScanner.getResources())
        .thenReturn(List.of(cluster));
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).connectedClusters(List.of(cluster));
    assertNotNull(distributedLogs.getStatus());
    assertNotNull(distributedLogs.getStatus().getConnectedClusters());
    assertEquals(distributedLogs.getStatus().getConnectedClusters().size(), 1);
    assertEquals(
        cluster.getMetadata().getName(),
        distributedLogs.getStatus().getConnectedClusters().get(0).getName());
    assertEquals(
        cluster.getMetadata().getNamespace(),
        distributedLogs.getStatus().getConnectedClusters().get(0).getNamespace());
    assertNotNull(distributedLogs.getStatus().getConnectedClusters().get(0).getConfig());
    assertEquals(
        distributedLogs.getMetadata().getNamespace() + "." + distributedLogs.getMetadata().getName(),
        distributedLogs.getStatus().getConnectedClusters().get(0).getConfig().getSgDistributedLogs());
    assertEquals(
        "P1M",
        distributedLogs.getStatus().getConnectedClusters().get(0).getConfig().getRetention());
  }

}
