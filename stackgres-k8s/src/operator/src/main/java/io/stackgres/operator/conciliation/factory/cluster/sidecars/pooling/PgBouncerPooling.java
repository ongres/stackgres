/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
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
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Sidecar(StackGresContainer.PGBOUNCER)
@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.PGBOUNCER)
public class PgBouncerPooling implements ContainerFactory<ClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final ContainerUserOverrideMounts containerUserOverrideMounts;
  private final PostgresSocketMount postgresSocket;
  private final ScriptTemplatesVolumeMounts scriptTemplatesVolumeMounts;

  @Inject
  protected PgBouncerPooling(LabelFactoryForCluster<StackGresCluster> labelFactory,
      ContainerUserOverrideMounts containerUserOverrideMounts,
      PostgresSocketMount postgresSocket,
      ScriptTemplatesVolumeMounts scriptTemplatesVolumeMounts) {
    this.labelFactory = labelFactory;
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
    this.scriptTemplatesVolumeMounts = scriptTemplatesVolumeMounts;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return StackGresVolume.PGBOUNCER_CONFIG.getResourceName(name);
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.PGBOUNCER_VERSION_KEY,
        StackGresComponent.PGBOUNCER.get(context.getClusterContext().getCluster())
        .getLatestVersion());
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return !Optional.ofNullable(context)
        .map(ClusterContainerContext::getClusterContext)
        .map(StackGresClusterContext::getSource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableConnectionPooling)
        .orElse(false);
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.PGBOUNCER.getName())
        .withImage(StackGresComponent.PGBOUNCER.get(context.getClusterContext().getCluster())
            .getLatestImageName())
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_START_PGBOUNCER_SH_PATH.filename())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withEnv(
            ClusterPath.PGBOUNCER_CONFIG_FILE_PATH.envVar(),
            ClusterPath.PGBOUNCER_CONFIG_UPDATED_FILE_PATH.envVar())
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(scriptTemplatesVolumeMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.PGBOUNCER_CONFIG.getName())
                .withMountPath(ClusterPath.PGBOUNCER_CONFIG_PATH.path())
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.PGBOUNCER_DYNAMIC_CONFIG.getName())
                .withMountPath(ClusterPath.PGBOUNCER_AUTH_PATH.path())
                .withSubPath(ClusterPath.PGBOUNCER_AUTH_PATH.filename())
                .withReadOnly(true)
                .build())
        .build();
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(@NotNull StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build(),
        ImmutableVolumePair.builder()
            .volume(buildAuthFileVolume())
            .build(),
        ImmutableVolumePair.builder()
            .volume(buildSecretVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.PGBOUNCER_CONFIG.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .withDefaultMode(0440)
            .build())
        .build();
  }

  private HasMetadata buildSource(@NotNull StackGresClusterContext context) {
    final StackGresCluster sgCluster = context.getSource();

    String configFile = getConfigFile(context);
    Map<String, String> data = Map.of(
        ClusterPath.PGBOUNCER_CONFIG_FILE_PATH.filename(), configFile);

    String namespace = sgCluster.getMetadata().getNamespace();
    String configMapName = configName(context);

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.genericLabels(sgCluster))
        .endMetadata()
        .withData(data)
        .build();
  }

  private Map<String, String> getDefaultParameters() {
    return Map.ofEntries(
        Map.entry("listen_port", Integer.toString(EnvoyUtil.PG_POOL_PORT)),
        Map.entry("unix_socket_dir", ClusterPath.PG_RUN_PATH.path()),
        Map.entry("auth_file", ClusterPath.PGBOUNCER_AUTH_FILE_PATH.path()));
  }

  private Volume buildAuthFileVolume() {
    return new VolumeBuilder()
        .withName(StackGresVolume.PGBOUNCER_DYNAMIC_CONFIG.getName())
        .withEmptyDir(new EmptyDirVolumeSourceBuilder()
            .build())
        .build();
  }

  private Volume buildSecretVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.PGBOUNCER_SECRETS.getName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(PatroniSecret.name(context))
            .build())
        .build();
  }

  private String getConfigFile(StackGresClusterContext context) {
    return ""
        + getDatabaseSection(context)
        + getUserSection(context)
        + getPgBouncerSection(context);
  }

  private String getPgBouncerSection(StackGresClusterContext context) {
    var newParams = context.getPoolingConfig()
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getPgbouncer)
        .orElseGet(HashMap::new);

    // Blocklist removal
    PgBouncerBlocklist.getBlocklistParameters().forEach(newParams::remove);

    Map<String, String> parameters = new HashMap<>(PgBouncerDefaultValues.getDefaultValues(
        StackGresVersion.getStackGresVersion(context.getCluster())));

    boolean isEnvoyDisabled = Optional.of(context.getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);
    parameters.put("listen_addr", isEnvoyDisabled ? "*" : "127.0.0.1");
    parameters.put("listen_port", String.valueOf(EnvoyUtil.PG_POOL_PORT));

    parameters.putAll(getDefaultParameters());
    parameters.putAll(newParams);

    String pgBouncerConfig = parameters.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    return "[pgbouncer]\n" + pgBouncerConfig + "\n";
  }

  private String getUserSection(StackGresClusterContext context) {
    var users = context.getPoolingConfig()
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getUsers)
        .orElseGet(HashMap::new);

    return !users.isEmpty()
        ? "[users]\n" + getSections(users) + "\n\n"
        : "";
  }

  private String getDatabaseSection(StackGresClusterContext context) {
    var databases = context.getPoolingConfig()
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getDatabases)
        .orElseGet(HashMap::new);

    return "[databases]\n\n"
        + (!databases.isEmpty() ? getSections(databases) + "\n\n" : "")
        + "* = port=" + EnvoyUtil.PG_PORT + "\n\n";
  }

  private String getSections(Map<String, Map<String, String>> sections) {
    return sections.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + " = " + entry.getValue().entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(" ")))
        .collect(Collectors.joining("\n"));
  }
}
