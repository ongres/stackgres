/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPgPooling
    implements ContainerFactory<StackGresClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  private static final String NAME = "pgbouncer";
  private static final Map<String, String> DEFAULT_PARAMETERS = ImmutableMap
      .<String, String>builder()
      .put("listen_port", Integer.toString(EnvoyUtil.PG_POOL_PORT))
      .put("unix_socket_dir", ClusterStatefulSetPath.PG_RUN_PATH.path())
      .build();
  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  protected AbstractPgPooling(
      LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return StatefulSetDynamicVolumes.PG_BOUNCER.getResourceName(name);
  }

  @Override
  public boolean isActivated(StackGresClusterContainerContext context) {
    return Optional.ofNullable(context.getClusterContext().getSource().getSpec())
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .map(disable -> !disable)
        .orElse(true);
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
        .withName(StatefulSetDynamicVolumes.PG_BOUNCER.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresClusterContext context) {
    final StackGresCluster stackGresCluster = context.getSource();
    Optional<StackGresPoolingConfig> pgbouncerConfig = context.getPoolingConfig();
    Map<String, String> params = getParameters(pgbouncerConfig);
    Map<String, String> pgbouncerIniParams = new LinkedHashMap<>(DEFAULT_PARAMETERS);
    pgbouncerIniParams.putAll(params);

    String pgBouncerConfig = pgbouncerIniParams.entrySet().stream()
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    String configFile = "[databases]\n"
        + " * = port = " + EnvoyUtil.PG_PORT + "\n"
        + "\n"
        + "[pgbouncer]\n"
        + pgBouncerConfig
        + "\n";
    Map<String, String> data = ImmutableMap.of("pgbouncer.ini", configFile);

    String namespace = stackGresCluster.getMetadata().getNamespace();
    String configMapName = configName(context);

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.clusterLabels(stackGresCluster))
        .endMetadata()
        .withData(data)
        .build();
  }

  protected abstract Map<String, String> getParameters(
      Optional<StackGresPoolingConfig> pgbouncerConfig);

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withVolumeMounts(getVolumeMounts(context))
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.PGBOUNCER_VERSION_KEY,
        StackGresComponent.PGBOUNCER.findLatestVersion());
  }

  protected String getImageName() {
    return StackGresComponent.PGBOUNCER.findLatestImageName();
  }

  protected abstract List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context);

}
