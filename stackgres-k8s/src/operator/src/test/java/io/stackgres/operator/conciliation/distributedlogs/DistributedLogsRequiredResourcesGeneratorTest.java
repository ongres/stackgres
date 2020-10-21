/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.ClusterScanner;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DistributedLogsRequiredResourcesGeneratorTest {

  @Inject
  DistributedLogsRequiredResourcesGenerator generator;

  @InjectMock
  ClusterScanner clusterScanner;

  List<StackGresCluster> connectedClusters;

  StackGresDistributedLogs distributedLogsCluster;

  @BeforeEach
  void setUp() {
    connectedClusters = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class)
        .getItems();

    when(clusterScanner.getResources()).thenReturn(connectedClusters);

    distributedLogsCluster = JsonUtil.readFromJson("distributedlogs/default.json",
        StackGresDistributedLogs.class);

  }

  @Test
  void getRequiredResources() {

    generator.getRequiredResources(distributedLogsCluster);
  }
}