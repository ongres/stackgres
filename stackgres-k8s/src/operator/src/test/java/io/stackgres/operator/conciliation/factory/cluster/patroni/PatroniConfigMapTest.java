/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniInitialConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniConfigMapTest {

  private static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();
  private final LabelFactoryForCluster<StackGresCluster> labelFactory = new ClusterLabelFactory(
      new ClusterLabelMapper());
  @Mock
  private StackGresClusterContext context;
  private PatroniConfigMap generator;
  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    generator = new PatroniConfigMap(labelFactory, JSON_MAPPER,
        new YamlMapperProvider());

    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().getAnnotations()
        .put(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
  }

  @Test
  void getConfigMapWithoutScope_shouldReturnExpectedEnvVars() throws Exception {
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    ConfigMap configMap = generator.buildSource(context);

    assertEquals(cluster.getMetadata().getName(),
        configMap.getData().get("PATRONI_SCOPE"));
    assertEquals(JSON_MAPPER.writeValueAsString(labelFactory.patroniClusterLabels(cluster)),
        configMap.getData().get("PATRONI_KUBERNETES_LABELS"));
    assertEquals("",
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

  @Test
  void getConfigMapWithoutEmptyPatroniConfig_shouldReturnExpectedEnvVars() throws Exception {
    cluster.getSpec().getConfiguration().setPatroni(new StackGresClusterPatroni());
    cluster.getSpec().getConfiguration().getPatroni()
        .setInitialConfig(new StackGresClusterPatroniInitialConfig());

    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    ConfigMap configMap = generator.buildSource(context);

    assertEquals(cluster.getMetadata().getName(),
        configMap.getData().get("PATRONI_SCOPE"));
    assertEquals(JSON_MAPPER.writeValueAsString(labelFactory.patroniClusterLabels(cluster)),
        configMap.getData().get("PATRONI_KUBERNETES_LABELS"));
    assertEquals("",
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

  @Test
  void getConfigMapWithScope_shouldReturnExpectedEnvVars() throws Exception {
    cluster.getSpec().getConfiguration().setPatroni(new StackGresClusterPatroni());
    cluster.getSpec().getConfiguration().getPatroni()
        .setInitialConfig(new StackGresClusterPatroniInitialConfig());
    cluster.getSpec().getConfiguration().getPatroni()
        .getInitialConfig().put("scope", "test");
    cluster.getSpec().getConfiguration().getPatroni()
        .getInitialConfig().put("test", true);

    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    ConfigMap configMap = generator.buildSource(context);

    assertEquals("test",
        configMap.getData().get("PATRONI_SCOPE"));
    assertEquals(JSON_MAPPER.writeValueAsString(labelFactory.patroniClusterLabels(cluster)),
        configMap.getData().get("PATRONI_KUBERNETES_LABELS"));
    assertEquals("test: true\n",
        configMap.getData().get("PATRONI_INITIAL_CONFIG"));
  }

}
