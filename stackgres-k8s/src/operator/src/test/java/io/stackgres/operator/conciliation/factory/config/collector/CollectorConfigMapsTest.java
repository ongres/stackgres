/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.common.ObservedClusterContext.CollectorPodContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectorConfigMapsTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private CollectorConfigMaps collectorConfigMaps;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    collectorConfigMaps = new CollectorConfigMaps(labelFactory, new YamlMapperProvider());
    config = Fixtures.config().loadDefault().get();
    setupCollectorConfig();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getConfig()).thenReturn(config);
  }

  private void setupCollectorConfig() {
    if (config.getSpec().getCollector() == null) {
      StackGresConfigCollector collector = new StackGresConfigCollector();
      StackGresConfigCollectorConfig collectorConfig = new StackGresConfigCollectorConfig();
      collectorConfig.put("receivers", new io.stackgres.common.crd.JsonObject());
      collectorConfig.put("exporters", new io.stackgres.common.crd.JsonObject());
      collectorConfig.put("service", new io.stackgres.common.crd.JsonObject());
      collectorConfig.getObjectOrPut("service").put("pipelines",
          new io.stackgres.common.crd.JsonObject());
      collector.setConfig(collectorConfig);
      config.getSpec().setCollector(collector);
    }
  }

  private ObservedClusterContext createObservedClusterContext() {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMeta());
    cluster.getMetadata().setNamespace("test-ns");
    cluster.getMetadata().setName("test-cluster");
    cluster.setSpec(new StackGresClusterSpec());
    CollectorPodContext pod = new CollectorPodContext(
        "test-ns", "test-cluster-0", Instant.now(), "10.0.0.1");
    return new ObservedClusterContext(cluster, List.of(pod));
  }

  @Test
  void generateResource_whenCollectorDeployedAndClustersExist_shouldGenerateConfigMap() {
    ObservedClusterContext observedCluster = createObservedClusterContext();
    when(context.getObservedClusters()).thenReturn(List.of(observedCluster));

    List<HasMetadata> resources = collectorConfigMaps.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertInstanceOf(ConfigMap.class, resources.getFirst());
  }

  @Test
  void generateResource_whenNoObservedClusters_shouldReturnEmpty() {
    when(context.getObservedClusters()).thenReturn(List.of());

    List<HasMetadata> resources = collectorConfigMaps.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenDeployCollectorFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setCollector(false);

    List<HasMetadata> resources = collectorConfigMaps.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

}
