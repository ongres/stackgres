/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.Volume;
import io.stackgres.common.crd.VolumeBuilder;
import io.stackgres.common.crd.VolumeMountBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Sidecar(StackGresContainer.POSTGRES_EXPORTER)
@OperatorVersionBinder(startAt = StackGresVersion.V_1_12)
@RunningContainer(StackGresContainer.POSTGRES_EXPORTER)
public class PostgresExporter implements ContainerFactory<ClusterContainerContext>,
        VolumeFactory<StackGresClusterContext> {

  public static final String POSTGRES_EXPORTER_PORT_NAME = "pgexporter";
  public static final String POSTGRES_EXPORTER_PGBOUNCER_QUERIES_PREFIX = "pgbouncer";
  public static final int POSTGRES_EXPORTER_PORT = 9187;

  private static final Logger POSTGRES_EXPORTER_LOGGER = LoggerFactory.getLogger(
      "io.stackgres.prometheus-postgres-exporter");
  public static final String QUERIES_YAML = "queries.yaml";

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final ContainerUserOverrideMounts containerUserOverrideMounts;
  private final PostgresSocketMount postgresSocket;
  private final ScriptTemplatesVolumeMounts scriptTemplatesVolumeMounts;
  protected final YamlMapperProvider yamlMapperProvider;

  @Inject
  public PostgresExporter(LabelFactoryForCluster<StackGresCluster> labelFactory,
      ContainerUserOverrideMounts containerUserOverrideMounts, PostgresSocketMount postgresSocket,
      ScriptTemplatesVolumeMounts scriptTemplatesVolumeMounts, YamlMapperProvider yamlMapperProvider) {
    super();
    this.labelFactory = labelFactory;
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
    this.scriptTemplatesVolumeMounts = scriptTemplatesVolumeMounts;
    this.yamlMapperProvider = yamlMapperProvider;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    final String name = clusterContext.getSource().getMetadata().getName();
    return StackGresVolume.EXPORTER_QUERIES.getResourceName(name);
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return !Optional.of(context.getClusterContext().getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableMetricsExporter)
        .orElse(false);
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    StackGresCluster cluster = context.getClusterContext().getSource();
    String superuserUsername =
        PatroniSecret.getSuperuserCredentials(context.getClusterContext()).v1;
    ContainerBuilder container = new ContainerBuilder();
    container.withName(StackGresContainer.POSTGRES_EXPORTER.getName())
        .withImage(StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER.get(cluster)
            .getLatestImageName())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_START_POSTGRES_EXPORTER_SH_PATH.filename())
        .withEnv(
            new EnvVarBuilder()
                .withName("PGAPPNAME")
                .withValue(StackGresContainer.POSTGRES_EXPORTER.getName())
                .build(),
            new EnvVarBuilder()
                .withName("DATA_SOURCE_NAME")
                .withValue("postgresql://" + superuserUsername + "@:" + EnvoyUtil.PG_PORT
                    + "/postgres"
                    + "?host=" + ClusterPath.PG_RUN_PATH.path()
                    + "&sslmode=disable")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_EXTEND_QUERY_PATH")
                .withValue("/var/opt/postgres-exporter/queries.yaml")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_CONSTANT_LABELS")
                .withValue("cluster_name=" + cluster.getMetadata().getName()
                    + ", namespace=" + cluster.getMetadata().getNamespace())
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_LOG_LEVEL")
                .withValue(POSTGRES_EXPORTER_LOGGER.isTraceEnabled() ? "debug" : "info")
                .build(),
            new EnvVarBuilder()
                .withName("PG_PORT")
                .withValue(String.valueOf(EnvoyUtil.PG_PORT))
                .build())
        .withPorts(new ContainerPortBuilder()
            .withProtocol("TCP")
            .withName(POSTGRES_EXPORTER_PORT_NAME)
            .withContainerPort(POSTGRES_EXPORTER_PORT)
            .build())
        .addAllToEnv(postgresSocket.getDerivedEnvVars(context))
        .addAllToEnv(scriptTemplatesVolumeMounts.getDerivedEnvVars(context))
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addAllToVolumeMounts(scriptTemplatesVolumeMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.EXPORTER_QUERIES.getName())
                .withMountPath("/var/opt/postgres-exporter/queries.yaml")
                .withSubPath("queries.yaml")
                .withReadOnly(true)
                .build()
        )
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context));

    return container.build();
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.PROMETHEUS_POSTGRES_EXPORTER_VERSION_KEY,
        StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER
        .get(context.getClusterContext().getCluster()).getLatestVersion());
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.EXPORTER_QUERIES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build();
  }

  private HasMetadata buildSource(StackGresClusterContext context) {

    final ConfigMapBuilder builder = new ConfigMapBuilder()
            .withNewMetadata()
            .withName(configName(context))
            .withNamespace(context.getSource().getMetadata().getNamespace())
            .withLabels(labelFactory.genericLabels(context.getSource()))
            .endMetadata();

    final boolean pgBouncerIsDisabled = Optional.of(context)
            .map(StackGresClusterContext::getCluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPods)
            .map(StackGresClusterPods::getDisableConnectionPooling)
            .orElse(false);

    if (pgBouncerIsDisabled) {
      final YAMLMapper yamlMapper = yamlMapperProvider.get();
      final ObjectNode queries;
      final String data;
      try {
        queries = (ObjectNode) yamlMapper
                .readTree(PostgresExporter.class.getResource("/prometheus-postgres-exporter/queries.yaml"));

        var fieldNames = Seq.seq(queries.fieldNames()).toList();
        for (var fieldName : fieldNames) {
          if (fieldName.startsWith(POSTGRES_EXPORTER_PGBOUNCER_QUERIES_PREFIX)) {
            queries.remove(fieldName);
          }
        }

        data = yamlMapper.writeValueAsString(queries);

        builder.withData(Map.of(QUERIES_YAML, data));

        return builder.build();
      } catch (JsonProcessingException e) {
        throw new RuntimeException("couldn't serialize prometheus postgres exporter queries to a string", e);
      } catch (IOException e) {
        throw new RuntimeException("couldn't read prometheus postgres exporter queries file", e);
      }
    }

    return builder
            .withData(Map.of(QUERIES_YAML,
                    Unchecked.supplier(() ->
                            Resources
                                    .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
                                            "/prometheus-postgres-exporter/queries.yaml")),
                                            StandardCharsets.UTF_8).read()).get()))
            .build();
  }

}
