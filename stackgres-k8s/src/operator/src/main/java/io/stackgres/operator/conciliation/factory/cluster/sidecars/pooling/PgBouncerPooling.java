/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Sidecar(StackGresContainer.PGBOUNCER)
@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.PGBOUNCER)
public class PgBouncerPooling implements ContainerFactory<ClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;
  private final UserOverrideMounts userOverrideMounts;
  private final PostgresSocketMounts postgresSocketMounts;
  private final TemplatesMounts templatesMounts;
  private final DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  @Inject
  protected PgBouncerPooling(
      LabelFactoryForCluster labelFactory,
      UserOverrideMounts userOverrideMounts,
      PostgresSocketMounts postgresSocketMounts,
      TemplatesMounts templatesMounts,
      DefaultPoolingConfigFactory defaultPoolingConfigFactory) {
    this.labelFactory = labelFactory;
    this.userOverrideMounts = userOverrideMounts;
    this.postgresSocketMounts = postgresSocketMounts;
    this.templatesMounts = templatesMounts;
    this.defaultPoolingConfigFactory = defaultPoolingConfigFactory;
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
            ClusterPath.PGBOUNCER_CONFIG_UPDATED_FILE_PATH.envVar(),
            ClusterPath.PGBOUNCER_AUTH_PATH.envVar(),
            ClusterPath.PGBOUNCER_AUTH_FILE_PATH.envVar(),
            ClusterPath.PGBOUNCER_AUTH_TEMPLATE_FILE_PATH.envVar())
        .withPorts(getContainerPorts(context.getClusterContext().getCluster()))
        .addAllToVolumeMounts(postgresSocketMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(userOverrideMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(templatesMounts.getVolumeMounts(context))
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
            .withReadOnly(false)
            .build(),
            new VolumeMountBuilder()
            .withName(StackGresVolume.PGBOUNCER_SECRETS.getName())
            .withMountPath(ClusterPath.PGBOUNCER_AUTH_TEMPLATE_FILE_PATH.path())
            .withSubPath(StackGresPasswordKeys.PGBOUNCER_USERS_KEY)
            .withReadOnly(true)
            .build(),
            new VolumeMountBuilder()
            .withName(StackGresVolume.POSTGRES_SSL_COPY.getName())
            .withMountPath(ClusterPath.SSL_PATH.path())
            .withReadOnly(true)
            .build())
     .build();
  }

  private List<ContainerPort> getContainerPorts(StackGresCluster cluster) {
    boolean isEnvoyDisabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);
    if (!isEnvoyDisabled) {
      return List.of();
    }
    return List.of(
        new ContainerPortBuilder()
            .withProtocol("TCP")
            .withName(EnvoyUtil.POSTGRES_PORT_NAME)
            .withContainerPort(EnvoyUtil.PG_POOL_PORT)
            .build(),
        new ContainerPortBuilder()
            .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
            .withProtocol("TCP")
            .withContainerPort(EnvoyUtil.PATRONI_PORT)
            .build());
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
    StackGresPoolingConfig poolingConfig = context.getPoolingConfig()
        .orElseGet(() -> defaultPoolingConfigFactory.buildResource(context.getSource()));
    return ""
        + getDatabaseSection(context, poolingConfig)
        + getUserSection(context, poolingConfig)
        + getPgBouncerSection(context, poolingConfig);
  }

  private String getPgBouncerSection(
      StackGresClusterContext context,
      StackGresPoolingConfig poolingConfig) {
    var newParams = Optional.of(poolingConfig)
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getPgbouncer)
        .map(HashMap::new)
        .orElseGet(HashMap::new);

    Optional.ofNullable(context.getCluster().getSpec().getConfigurations().getPooling())
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getPgbouncer)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .forEach(entry -> newParams.put(entry.getKey(), entry.getValue()));

    // Blocklist removal
    PgBouncerBlocklist.getBlocklistParameters().forEach(newParams::remove);

    Map<String, String> parameters = new HashMap<>(PgBouncerDefaultValues.getDefaultValues(
        StackGresVersion.getStackGresVersion(context.getCluster())));

    boolean isEnvoyDisabled = Optional.of(context.getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);
    parameters.put("listen_addr", isEnvoyDisabled ? "0.0.0.0,::" : "127.0.0.1,::1");
    parameters.put("listen_port", String.valueOf(EnvoyUtil.PG_POOL_PORT));
    parameters.put("unix_socket_dir", ClusterPath.PG_RUN_PATH.path());
    parameters.put("auth_file", ClusterPath.PGBOUNCER_AUTH_FILE_PATH.path());
    if (Optional.of(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      parameters.put("client_tls_sslmode", "prefer");
      parameters.put("client_tls_cert_file",
          ClusterPath.SSL_PATH.path() + "/" + PatroniUtil.CERTIFICATE_KEY);
      parameters.put("client_tls_key_file",
          ClusterPath.SSL_PATH.path() + "/" + PatroniUtil.PRIVATE_KEY_KEY);
    }
    parameters.putAll(newParams);

    String pgBouncerConfig = parameters.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    return "[pgbouncer]\n" + pgBouncerConfig + "\n";
  }

  private String getUserSection(
      StackGresClusterContext context,
      StackGresPoolingConfig poolingConfig) {
    var users = Optional.of(poolingConfig)
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getUsers)
        .map(HashMap::new)
        .orElseGet(HashMap::new);

    Optional.ofNullable(context.getCluster().getSpec().getConfigurations().getPooling())
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getUsers)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .forEach(entry -> users.put(entry.getKey(), entry.getValue()));

    return !users.isEmpty()
        ? "[users]\n" + getSections(users) + "\n\n"
        : "";
  }

  private String getDatabaseSection(
      StackGresClusterContext context,
      StackGresPoolingConfig poolingConfig) {
    var databases = Optional.of(poolingConfig)
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getDatabases)
        .map(HashMap::new)
        .orElseGet(HashMap::new);

    Optional.ofNullable(context.getCluster().getSpec().getConfigurations().getPooling())
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getDatabases)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .forEach(entry -> databases.put(entry.getKey(), entry.getValue()));

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
