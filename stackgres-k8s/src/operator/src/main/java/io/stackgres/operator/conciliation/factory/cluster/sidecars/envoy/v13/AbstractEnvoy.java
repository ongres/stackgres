/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.v13;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEnvoy implements ContainerFactory<ClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  public static final String SERVICE_MONITOR = "-stackgres-envoy";
  public static final String SERVICE = "-envoyexp";

  private static final String CONFIG_SUFFIX = "-envoy-config";
  protected static final Logger ENVOY_LOGGER = LoggerFactory.getLogger("io.stackgres.envoy");
  protected static final ImmutableMap<String, Integer> LISTEN_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_entry_port", EnvoyUtil.PG_ENTRY_PORT,
          "postgres_repl_entry_port", EnvoyUtil.PG_REPL_ENTRY_PORT,
          "babelfish_entry_port", EnvoyUtil.BF_ENTRY_PORT,
          "patroni_entry_port", EnvoyUtil.PATRONI_ENTRY_PORT);
  protected static final ImmutableMap<String, Integer> CLUSTER_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_pool_port", EnvoyUtil.PG_POOL_PORT,
          "postgres_port", EnvoyUtil.PG_PORT,
          "babelfish_port", EnvoyUtil.BF_PORT,
          "patroni_port", EnvoyUtil.PATRONI_PORT);

  protected final YamlMapperProvider yamlMapperProvider;
  protected final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  protected AbstractEnvoy(YamlMapperProvider yamlMapperProvider,
                       LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.yamlMapperProvider = yamlMapperProvider;
    this.labelFactory = labelFactory;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  public static String serviceMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getSource().getMetadata().getNamespace();
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + SERVICE_MONITOR);
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.ENVOY_VERSION_KEY,
        StackGresComponent.ENVOY.get(context.getClusterContext().getCluster())
        .getLatestVersion());
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Seq.<VolumePair>of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build())
        .append(buildExtraVolumes(context));
  }

  protected abstract Stream<ImmutableVolumePair> buildExtraVolumes(StackGresClusterContext context);

  protected Volume buildVolume(StackGresClusterContext context) {
    final String clusterName = context.getSource().getMetadata().getName();
    return new VolumeBuilder()
        .withName(StackGresVolume.ENVOY.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withDefaultMode(420)
            .withName(StackGresVolume.ENVOY.getResourceName(clusterName))
            .build())
        .build();
  }

  protected abstract HasMetadata buildSource(StackGresClusterContext context);

  protected abstract String getEnvoyConfigPath(final StackGresCluster stackGresCluster,
      boolean disablePgBouncer);

  public abstract List<VolumeMount> getVolumeMounts(ClusterContainerContext context);

}
