/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import java.util.HashMap;
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
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.pgbouncer.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.pgbouncer.StackGresPoolingConfigPgBouncerDatabases;
import io.stackgres.common.crd.sgpooling.pgbouncer.StackGresPoolingConfigPgBouncerUsers;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

public abstract class AbstractPgPooling
    implements ContainerFactory<StackGresClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  private static final String NAME = "pgbouncer";

  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  protected AbstractPgPooling(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return StatefulSetDynamicVolumes.PG_BOUNCER.getResourceName(name);
  }

  @Override
  public boolean isActivated(StackGresClusterContainerContext context) {
    return Optional.ofNullable(context)
        .map(StackGresClusterContainerContext::getClusterContext)
        .map(StackGresClusterContext::getSource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .map(disable -> !disable)
        .orElse(Boolean.TRUE);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(@NotNull StackGresClusterContext context) {
    return Stream.of(ImmutableVolumePair.builder()
        .volume(buildVolume(context))
        .source(buildSource(context))
        .build());
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.PG_BOUNCER.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(@NotNull StackGresClusterContext context) {
    final StackGresCluster sgCluster = context.getSource();

    Optional<StackGresPoolingConfigPgBouncer> pgbouncerConfig = context.getPoolingConfig()
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer);

    var parameters = pgbouncerConfig
        .map(StackGresPoolingConfigPgBouncer::getParameters)
        .orElseGet(HashMap::new);

    var databases = pgbouncerConfig
        .map(StackGresPoolingConfigPgBouncer::getDatabases)
        .orElseGet(HashMap::new);

    var users = pgbouncerConfig
        .map(StackGresPoolingConfigPgBouncer::getUsers)
        .orElseGet(HashMap::new);

    String configFile = ""
        + getDatabaseSection(databases)
        + getUserSection(users)
        + getPgBouncerSection(parameters);

    Map<String, String> data = ImmutableMap.of("pgbouncer.ini", configFile);

    String namespace = sgCluster.getMetadata().getNamespace();
    String configMapName = configName(context);

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.clusterLabels(sgCluster))
        .endMetadata()
        .withData(data)
        .build();
  }

  private String getPgBouncerSection(Map<String, String> params) {
    // Blocklist removal
    Blocklist.getBlocklistParameters().forEach(bl -> params.remove(bl));

    var parameters = new LinkedHashMap<>(DefaultValues.getDefaultValues());
    parameters.putAll(params);

    String pgBouncerConfig = parameters.entrySet().stream()
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    return "[pgbouncer]\n" + pgBouncerConfig + "\n";
  }

  private String getUserSection(Map<String, StackGresPoolingConfigPgBouncerUsers> users) {
    final String usersSection = users.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> {
          final String params = Seq.of("pool_mode", "max_user_connections")
              .map(param -> StackGresUtil.mapMethodParameterValues(param, entry.getValue()))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .reduce((first, second) -> first + " " + second)
              .orElse("");
          return entry.getKey() + " = " + params;
        })
        .collect(Collectors.joining("\n"));
    return !users.isEmpty()
        ? "[users]\n" + usersSection + "\n\n"
        : "";
  }

  private String getDatabaseSection(
      Map<String, StackGresPoolingConfigPgBouncerDatabases> databases) {
    final String databasesSection = databases.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> {
          final String params = Seq.of("dbname", "pool_size", "reserve_pool", "pool_mode",
              "max_db_connections", "client_encoding", "datestyle", "timezone")
              .map(param -> StackGresUtil.mapMethodParameterValues(param, entry.getValue()))
              .concat(Optional.of("port=" + EnvoyUtil.PG_PORT))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .reduce((first, second) -> first + " " + second)
              .orElse("");
          return entry.getKey() + " = " + params;
        })
        .collect(Collectors.joining("\n"));
    return !databases.isEmpty()
        ? "[databases]\n" + databasesSection + "\n\n"
        : "[databases]\n" + "* = port=" + EnvoyUtil.PG_PORT + "\n\n";
  }

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
