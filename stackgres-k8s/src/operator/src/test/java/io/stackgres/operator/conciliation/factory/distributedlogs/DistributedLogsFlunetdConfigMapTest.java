/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsFlunetdConfigMapTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsFlunetdConfigMap distributedLogsFlunetdConfigMap;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsFlunetdConfigMap = new DistributedLogsFlunetdConfigMap(labelFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);
  }

  @Test
  void generateResource_withNoConnectedClusters_shouldGenerateConfigMap() {
    when(context.getConnectedClusters()).thenReturn(List.of());

    List<HasMetadata> resources =
        distributedLogsFlunetdConfigMap.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof ConfigMap);
    ConfigMap configMap = (ConfigMap) resources.getFirst();
    assertEquals(
        DistributedLogsFlunetdConfigMap.configName(distributedLogs),
        configMap.getMetadata().getName());
    assertEquals("distributed-logs", configMap.getMetadata().getNamespace());
    assertNotNull(configMap.getData().get("fluentd.conf"));
  }

  @Test
  void generateResource_withConnectedClusters_shouldGenerateConfigMapWithClusterEntries() {
    StackGresCluster connectedCluster = new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("my-cluster")
        .withNamespace("my-namespace")
        .endMetadata()
        .build();
    when(context.getConnectedClusters()).thenReturn(List.of(connectedCluster));

    List<HasMetadata> resources =
        distributedLogsFlunetdConfigMap.generateResource(context).toList();

    assertEquals(1, resources.size());
    ConfigMap configMap = (ConfigMap) resources.getFirst();
    String fluentdConf = configMap.getData().get("fluentd.conf");
    assertNotNull(fluentdConf);
    assertTrue(fluentdConf.contains("my-namespace.my-cluster"),
        "Expected fluentd config to contain connected cluster tag name");
  }

  @Test
  void generateResource_workerCountScalesWithClusters_shouldHaveCorrectWorkerCount() {
    when(context.getConnectedClusters()).thenReturn(List.of());

    List<HasMetadata> resources =
        distributedLogsFlunetdConfigMap.generateResource(context).toList();

    ConfigMap configMap = (ConfigMap) resources.getFirst();
    String fluentdConf = configMap.getData().get("fluentd.conf");
    assertTrue(fluentdConf.contains("workers 16"),
        "Expected 16 workers for 0 connected clusters (batch size rounds up to 16)");

    StackGresCluster cluster1 = new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("cluster-1")
        .withNamespace("ns-1")
        .endMetadata()
        .build();
    StackGresCluster cluster2 = new StackGresClusterBuilder()
        .withNewMetadata()
        .withName("cluster-2")
        .withNamespace("ns-2")
        .endMetadata()
        .build();
    when(context.getConnectedClusters()).thenReturn(List.of(cluster1, cluster2));

    resources = distributedLogsFlunetdConfigMap.generateResource(context).toList();
    configMap = (ConfigMap) resources.getFirst();
    fluentdConf = configMap.getData().get("fluentd.conf");
    assertTrue(fluentdConf.contains("workers 16"),
        "Expected 16 workers for 2 connected clusters (1 core + 2 = 3, rounds up to 16)");
  }

}
