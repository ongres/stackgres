/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.comparator.DefaultComparator;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DistributedLogsRequiredResourcesGeneratorTest {

  @Inject
  DistributedLogsRequiredResourcesGenerator generator;

  @InjectMock
  ConnectedClustersScannerImpl clusterScanner;

  @Inject
  @ReconciliationScope(value = StackGresDistributedLogs.class, kind = "StatefulSet")
  DistributedLogsStatefulSetComparator stsComparator;

  DefaultComparator configMapComparator = new DefaultComparator();

  List<StackGresCluster> connectedClusters;

  StackGresDistributedLogs distributedLogsCluster;

  String randomNamespace = StringUtils.getRandomNamespace();
  String randomName = StringUtils.getRandomClusterName();
  String clusterUid = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    randomNamespace = StringUtils.getRandomNamespace();
    randomName = StringUtils.getRandomClusterName();
    clusterUid = UUID.randomUUID().toString();
    connectedClusters = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class)
        .getItems();
    connectedClusters.forEach(c -> {
      c.getMetadata().setName(StringUtils.getRandomClusterName());
      c.getMetadata().setNamespace(randomNamespace);
    });

    lenient().when(clusterScanner.getConnectedClusters(any())).thenReturn(connectedClusters);

    distributedLogsCluster = JsonUtil.readFromJson("distributedlogs/default.json",
        StackGresDistributedLogs.class);

  }

  @Test
  void getRequiredResources_shouldNotFail() {
    generator.getRequiredResources(distributedLogsCluster);
  }

}
