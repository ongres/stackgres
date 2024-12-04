/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class PatroniConfigMap implements VolumeFactory<StackGresClusterContext> {

  public static final String PATRONI_DCS_CONFIG_ENV_NAME = "PATRONI_DCS_CONFIG";
  public static final int PATRONI_LOG_FILE_SIZE = 256 * 1024 * 1024;

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("io.stackgres.patroni");

  private final LabelFactoryForCluster labelFactory;
  private final PatroniConfigEndpoints patroniConfigEndpoints;
  private final ObjectMapper objectMapper;
  private final YAMLMapper yamlMapper;

  @Inject
  public PatroniConfigMap(
      LabelFactoryForCluster labelFactory,
      @OperatorVersionBinder
      PatroniConfigEndpoints patroniConfigEndpoints,
      ObjectMapper objectMapper,
      YamlMapperProvider yamlMapperProvider) {
    this.labelFactory = labelFactory;
    this.patroniConfigEndpoints = patroniConfigEndpoints;
    this.objectMapper = objectMapper;
    this.yamlMapper = yamlMapperProvider.get();
  }

  public static String name(ClusterContext clusterContext) {
    return StackGresVolume.PATRONI_ENV
        .getResourceName(clusterContext.getCluster().getMetadata().getName());
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

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.PATRONI_ENV.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(0444)
            .build())
        .build();
  }

  public @NotNull ConfigMap buildSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    boolean isEnvoyDisabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_CONFIG_FILE", ClusterPath.PATRONI_CONFIG_FILE_PATH.path());
    data.put("PATRONI_INITIAL_CONFIG", PatroniUtil.getInitialConfig(
        cluster, labelFactory, yamlMapper, objectMapper));
    data.put(PATRONI_DCS_CONFIG_ENV_NAME, patroniConfigEndpoints.getPatroniConfigAsYamlString(context));
    data.put("PATRONI_PG_CTL_TIMEOUT", Optional.ofNullable(cluster.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .flatMap(StackGresClusterPatroniConfig::getPgCtlTimeout)
        .map(Object::toString)
        .orElse("60"));
    data.put("PATRONI_POSTGRESQL_LISTEN", (isEnvoyDisabled ? "0.0.0.0:" : "127.0.0.1:") + EnvoyUtil.PG_PORT);
    data.put("PATRONI_POSTGRESQL_CONNECT_ADDRESS",
        "${POD_IP}:" + (isEnvoyDisabled ? EnvoyUtil.PG_PORT : EnvoyUtil.PG_REPL_ENTRY_PORT));

    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:" + EnvoyUtil.PATRONI_PORT);
    data.put("PATRONI_POSTGRESQL_DATA_DIR", ClusterPath.PG_DATA_PATH.path());
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", ClusterPath.PG_RUN_PATH.path());

    if (Optional.ofNullable(cluster.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getSgDistributedLogs).isPresent()) {
      data.put("PATRONI_LOG_DIR", ClusterPath.PG_LOG_PATH.path());
      data.put("PATRONI_LOG_FILE_NUM", "2");
      data.put("PATRONI_LOG_FILE_SIZE", String.valueOf(PATRONI_LOG_FILE_SIZE));
    }

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    data.put("PATRONI_SCRIPTS",
        Optional.ofNullable(
            cluster.getSpec().getInitialData())
            .map(StackGresClusterInitialData::getScripts)
            .map(List::size)
            .map(String::valueOf)
            .orElse("0"));

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }
}
