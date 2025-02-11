/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.Volume;
import io.stackgres.common.crd.VolumeBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import io.stackgres.testutil.JsonUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresExporterTest {

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private UserOverrideMounts userOverrideMounts;

  @Mock
  private PostgresSocketMounts postgresSocketMounts;

  @Mock
  private TemplatesMounts templatesMounts;

  private final YamlMapperProvider yamlMapperProvider = new YamlMapperProvider();

  private PostgresExporter postgresExporter;

  @BeforeEach
  public void setupClass() {
    this.postgresExporter = new PostgresExporter(labelFactory,
        postgresSocketMounts, templatesMounts, userOverrideMounts, yamlMapperProvider);
  }

  @Test
  void ifDisableMetricsExporterIsNotSpecified_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    Assertions.assertTrue(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableMetricsExporterIsFalse_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getSource().getSpec().getPods().setDisableMetricsExporter(false);
    Assertions.assertTrue(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableMetricsExporterIsTrue_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getSource().getSpec().getPods().setDisableMetricsExporter(true);
    Assertions.assertFalse(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableConnectionPooling_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods().setDisableConnectionPooling(true);

    List<VolumePair> volumes = postgresExporter.buildVolumes(context.getClusterContext()).toList();

    Volume expected = new VolumeBuilder()
        .withName(StackGresVolume.EXPORTER_QUERIES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
          .withName(PostgresExporter.configName(context.getClusterContext()))
          .build())
        .build();

    Assertions.assertEquals(1L, volumes.size());
    Assertions.assertEquals(expected, volumes.getFirst().getVolume());
    Assertions.assertTrue(volumes.getFirst().getSource().isPresent());
    Assertions.assertInstanceOf(ConfigMap.class, volumes.getFirst().getSource().get());
    Assertions.assertEquals(
        expected.getConfigMap().getName(),
        volumes.getFirst().getSource().get().getMetadata().getName()
    );

    var sourceVolumeDataQueriesKeys = getSourceVolumeDataQueriesKeys(volumes);

    Assertions.assertFalse(sourceVolumeDataQueriesKeys.isEmpty());
    Assertions.assertFalse(
        sourceVolumeDataQueriesKeys.stream()
          .anyMatch(key -> key.startsWith(PostgresExporter.POSTGRES_EXPORTER_PGBOUNCER_QUERIES_PREFIX))
    );
  }

  @Test
  void ifEnabledConnectionPooling_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods().setDisableConnectionPooling(false);

    List<VolumePair> volumes = postgresExporter.buildVolumes(context.getClusterContext()).toList();

    Volume expected = new VolumeBuilder()
        .withName(StackGresVolume.EXPORTER_QUERIES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
          .withName(PostgresExporter.configName(context.getClusterContext()))
          .build())
        .build();

    Assertions.assertEquals(1L, volumes.size());
    Assertions.assertEquals(expected, volumes.getFirst().getVolume());
    Assertions.assertTrue(volumes.getFirst().getSource().isPresent());
    Assertions.assertInstanceOf(ConfigMap.class, volumes.getFirst().getSource().get());
    Assertions.assertEquals(
        expected.getConfigMap().getName(), volumes.getFirst().getSource().get().getMetadata().getName()
    );

    var sourceVolumeDataQueriesKeys = getSourceVolumeDataQueriesKeys(volumes);

    Assertions.assertFalse(sourceVolumeDataQueriesKeys.isEmpty());
    Assertions.assertTrue(
        sourceVolumeDataQueriesKeys.stream()
          .anyMatch(key -> key.startsWith(PostgresExporter.POSTGRES_EXPORTER_PGBOUNCER_QUERIES_PREFIX))
    );
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .config(getDefaultConfig())
            .source(getDefaultCluster())
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresProfile())
            .currentInstances(0)
            .build())
        .dataVolumeName("test")
        .build();
  }

  private StackGresConfig getDefaultConfig() {
    return Fixtures.config().loadDefault().get();
  }

  private StackGresCluster getDefaultCluster() {
    return Fixtures.cluster().loadDefault().get();
  }

  @NotNull
  private static List<String> getSourceVolumeDataQueriesKeys(List<VolumePair> volumes) {
    return volumes
      .getFirst()
      .getSource()
      .map(ConfigMap.class::cast)
      .map(ConfigMap::getData)
      .map(data -> data.get(PostgresExporter.QUERIES_YAML))
      .map(Unchecked.function(queries -> JsonUtil.yamlMapper().readTree(queries)))
      .map(JsonNode::fieldNames)
      .stream()
      .flatMap(Seq::seq)
      .toList();
  }
}
