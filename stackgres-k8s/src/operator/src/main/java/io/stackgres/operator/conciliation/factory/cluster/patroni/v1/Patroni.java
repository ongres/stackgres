/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v1;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.RegistryBinding;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.BackupMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.HugePagesMounts;
import io.stackgres.operator.conciliation.factory.cluster.PostgresEnvironmentVariables;
import io.stackgres.operator.conciliation.factory.cluster.ReplicateMounts;
import io.stackgres.operator.conciliation.factory.cluster.ReplicationInitializationMounts;
import io.stackgres.operator.conciliation.factory.cluster.RestoreMounts;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniEnvironmentVariables;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniMounts;
import io.stackgres.operator.conciliation.factory.cluster.v1.PostgresExtensionMounts;
import io.stackgres.operator.conciliation.factory.v1.PostgresDataMounts;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder(registry = RegistryBinding.DISABLED)
@RunningContainer(StackGresContainer.PATRONI)
public class Patroni implements ContainerFactory<ClusterContainerContext> {

  private static final String POD_MONITOR = "-patroni";

  private final PatroniEnvironmentVariables patroniEnvironmentVariables;
  private final PostgresEnvironmentVariables postgresEnvironmentVariables;
  private final PostgresSocketMounts postgresSocket;
  private final PostgresDataMounts postgresDataMounts;
  private final PostgresExtensionMounts postgresExtensions;
  private final TemplatesMounts templateMounts;
  private final UserOverrideMounts userOverrideMounts;
  private final LocalBinMounts localBinMounts;
  private final RestoreMounts restoreMounts;
  private final BackupMounts backupMounts;
  private final ReplicationInitializationMounts replicationInitMounts;
  private final ReplicateMounts replicateMounts;
  private final PatroniMounts patroniMounts;
  private final HugePagesMounts hugePagesMounts;
  private final PatroniConfigMap patroniConfigMap;

  public static String podMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getSource().getMetadata().getNamespace();
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + POD_MONITOR);
  }

  @Inject
  public Patroni(
      PatroniEnvironmentVariables patroniEnvironmentVariables,
      PostgresEnvironmentVariables postgresEnvironmentVariables,
      PostgresSocketMounts postgresSocket,
      PostgresDataMounts postgresDataMounts,
      PostgresExtensionMounts postgresExtensions,
      TemplatesMounts templateMounts,
      UserOverrideMounts userOverrideMounts,
      LocalBinMounts localBinMounts,
      RestoreMounts restoreMounts,
      BackupMounts backupMounts,
      ReplicationInitializationMounts replicationInitMounts,
      ReplicateMounts replicateMounts,
      PatroniMounts patroniMounts,
      HugePagesMounts hugePagesMounts,
      @OperatorVersionBinder
      PatroniConfigMap patroniConfigMap) {
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.postgresEnvironmentVariables = postgresEnvironmentVariables;
    this.postgresSocket = postgresSocket;
    this.postgresDataMounts = postgresDataMounts;
    this.postgresExtensions = postgresExtensions;
    this.templateMounts = templateMounts;
    this.userOverrideMounts = userOverrideMounts;
    this.localBinMounts = localBinMounts;
    this.restoreMounts = restoreMounts;
    this.backupMounts = backupMounts;
    this.replicationInitMounts = replicationInitMounts;
    this.replicateMounts = replicateMounts;
    this.patroniMounts = patroniMounts;
    this.hugePagesMounts = hugePagesMounts;
    this.patroniConfigMap = patroniConfigMap;
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresKeys.POSTGRES_VERSION_KEY,
        StackGresUtil.getPostgresFlavorComponent(context.getClusterContext().getCluster())
        .get(context.getClusterContext().getCluster())
        .getVersion(
            context.getClusterContext().getContext(),
            context.getClusterContext().getCluster().getStatus().getPostgresVersion()),
        StackGresKeys.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.get(context.getClusterContext().getCluster())
        .getLatestVersion(
            context.getClusterContext().getContext(),
            Map.of(
                StackGresUtil.getPostgresFlavorComponent(context.getClusterContext().getCluster())
                .get(context.getClusterContext().getCluster()),
                context.getClusterContext().getCluster().getStatus().getPostgresVersion())));
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getSource();
    final String patroniImageName = StackGresUtil.getPatroniImageName(
        clusterContext.getContext(), cluster);

    ImmutableList.Builder<VolumeMount> volumeMounts = ImmutableList.<VolumeMount>builder()
        .addAll(postgresSocket.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(StackGresVolume.DSHM.getName())
            .withMountPath(ClusterPathV1.SHARED_MEMORY_PATH.path())
            .build())
        .add(new VolumeMountBuilder()
            .withName(StackGresVolume.LOG.getName())
            .withMountPath(ClusterPathV1.PG_LOG_PATH.path())
            .build())
        .addAll(templateMounts.getVolumeMounts(context))
        .addAll(userOverrideMounts.getVolumeMounts(context))
        .addAll(localBinMounts.getVolumeMounts(context))
        .addAll(patroniMounts.getVolumeMounts(context))
        .addAll(backupMounts.getVolumeMounts(context))
        .addAll(replicationInitMounts.getVolumeMounts(context))
        .addAll(replicateMounts.getVolumeMounts(context))
        .addAll(postgresDataMounts.getVolumeMounts(context))
        .addAll(postgresExtensions.getVolumeMounts(context))
        .addAll(hugePagesMounts.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(StackGresVolume.POSTGRES_SSL_COPY.getName())
            .withMountPath(ClusterPathV1.SSL_PATH.path())
            .withReadOnly(true)
            .build());

    Optional.ofNullable(cluster.getSpec().getInitialData())
        .map(StackGresClusterInitialData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName).ifPresent(ignore ->
            volumeMounts.addAll(restoreMounts.getVolumeMounts(context))
        );

    boolean isEnvoyDisabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);
    final int patroniPort = isEnvoyDisabled ? EnvoyUtil.PATRONI_PORT : EnvoyUtil.PATRONI_ENTRY_PORT;

    return new ContainerBuilder()
        .withName(StackGresContainer.PATRONI.getName())
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            ClusterPathV1.TEMPLATES_PATH.path() + "/"
                + ClusterPathV1.LOCAL_BIN_START_PATRONI_SH_PATH.filename())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withVolumeMounts(volumeMounts.build())
        .withEnv(getEnvVars(context))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(clusterContext)).build())
            .build())
        .addToEnv(new EnvVarBuilder()
            .withName("PATRONI_CONFIG_MD5SUM")
            .withValue(Optional.of(patroniConfigMap.buildSource(context.getClusterContext()))
                .map(ConfigMap::getData)
                .map(data -> {
                  var dataWithoutDcs = new HashMap<>(data);
                  dataWithoutDcs.remove(PatroniConfigMap.PATRONI_DCS_CONFIG_ENV_NAME);
                  return StackGresUtil.addMd5Sum(dataWithoutDcs);
                })
                .map(data -> data.get(StackGresUtil.MD5SUM_2_KEY))
                .orElseThrow())
            .build())
        .withReadinessProbe(new ProbeBuilder(cluster.getSpec().getPods().getReadinessProbe())
            .withNewHttpGet()
            .withPath("/readiness")
            .withPort(new IntOrString(patroniPort))
            .withScheme("HTTP")
            .endHttpGet()
            .withInitialDelaySeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getReadinessProbe())
                .map(Probe::getInitialDelaySeconds)
                .orElse(0))
            .withPeriodSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getReadinessProbe())
                .map(Probe::getPeriodSeconds)
                .orElse(2))
            .withTimeoutSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getReadinessProbe())
                .map(Probe::getTimeoutSeconds)
                .orElse(1))
            .withFailureThreshold(
                Optional.ofNullable(cluster.getSpec().getPods().getReadinessProbe())
                .map(Probe::getFailureThreshold)
                .orElse(6))
            .build())
        .withStartupProbe(new ProbeBuilder(cluster.getSpec().getPods().getStartupProbe())
            .withNewHttpGet()
            .withPath("/liveness")
            .withPort(new IntOrString(EnvoyUtil.PATRONI_PORT))
            .withScheme("HTTP")
            .endHttpGet()
            .withPeriodSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getStartupProbe())
                .map(Probe::getPeriodSeconds)
                .orElse(5))
            .withTimeoutSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getStartupProbe())
                .map(Probe::getTimeoutSeconds)
                .orElse(2))
            .withFailureThreshold(
                Optional.ofNullable(cluster.getSpec().getPods().getStartupProbe())
                .map(Probe::getFailureThreshold)
                .orElse(180))
            .withSuccessThreshold(
                Optional.ofNullable(cluster.getSpec().getPods().getStartupProbe())
                .map(Probe::getSuccessThreshold)
                .orElse(1))
            .build())
        .withLivenessProbe(new ProbeBuilder(cluster.getSpec().getPods().getLivenessProbe())
            .withNewHttpGet()
            .withPath("/liveness")
            .withPort(new IntOrString(EnvoyUtil.PATRONI_PORT))
            .withScheme("HTTP")
            .endHttpGet()
            // The startup probe owns the startup window: Kubernetes only begins the liveness
            // initialDelay after the startup probe first succeeds, so a user-supplied delay here
            // would reopen an unprotected post-start window and break the fencing margin. Force 0.
            .withInitialDelaySeconds(0)
            .withPeriodSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getLivenessProbe())
                .map(Probe::getPeriodSeconds)
                .orElse(5))
            .withTimeoutSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getLivenessProbe())
                .map(Probe::getTimeoutSeconds)
                .orElse(2))
            .withFailureThreshold(
                Optional.ofNullable(cluster.getSpec().getPods().getLivenessProbe())
                .map(Probe::getFailureThreshold)
                .orElse(3))
            .withTerminationGracePeriodSeconds(
                Optional.ofNullable(cluster.getSpec().getPods().getLivenessProbe())
                .map(Probe::getTerminationGracePeriodSeconds)
                .orElse(3L))
            .build())
        .withPorts(getContainerPorts(cluster))
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
    boolean isConnectionPoolingDisabled = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableConnectionPooling)
        .orElse(false);
    if (getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      if (isConnectionPoolingDisabled) {
        return List.of(
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT)
                .build(),
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(EnvoyUtil.BABELFISH_PORT_NAME)
                .withContainerPort(EnvoyUtil.BF_PORT)
                .build(),
            new ContainerPortBuilder()
                .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_PORT)
                .build());
      }
      return List.of(
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
              .withContainerPort(EnvoyUtil.PG_PORT)
              .build(),
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.BABELFISH_PORT_NAME)
              .withContainerPort(EnvoyUtil.BF_PORT)
              .build(),
          new ContainerPortBuilder()
              .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
              .withProtocol("TCP")
              .withContainerPort(EnvoyUtil.PATRONI_PORT)
              .build());
    }
    if (isConnectionPoolingDisabled) {
      return List.of(
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.POSTGRES_PORT_NAME)
              .withContainerPort(EnvoyUtil.PG_PORT)
              .build(),
          new ContainerPortBuilder()
              .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
              .withProtocol("TCP")
              .withContainerPort(EnvoyUtil.PATRONI_PORT)
              .build());
    }
    return List.of(
        new ContainerPortBuilder()
            .withProtocol("TCP")
            .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
            .withContainerPort(EnvoyUtil.PG_PORT)
            .build(),
        new ContainerPortBuilder()
            .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
            .withProtocol("TCP")
            .withContainerPort(EnvoyUtil.PATRONI_PORT)
            .build());
  }

  private ImmutableList<EnvVar> getEnvVars(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    return ImmutableList.<EnvVar>builder()
        .addAll(patroniEnvironmentVariables.getEnvVars(clusterContext))
        .addAll(postgresEnvironmentVariables.getEnvVars(clusterContext))
        .add(new EnvVarBuilder()
            .withName(ClusterPathV1.TEMPLATES_PATH.name())
            .withValue(ClusterPathV1.TEMPLATES_PATH.path())
            .build())
        .addAll(localBinMounts.getDerivedEnvVars(context))
        .addAll(postgresDataMounts.getDerivedEnvVars(context))
        .addAll(postgresExtensions.getDerivedEnvVars(context))
        .addAll(patroniMounts.getDerivedEnvVars(context))
        .addAll(backupMounts.getDerivedEnvVars(context))
        .addAll(replicationInitMounts.getDerivedEnvVars(context))
        .addAll(replicateMounts.getDerivedEnvVars(context))
        .addAll(restoreMounts.getDerivedEnvVars(context))
        .addAll(hugePagesMounts.getDerivedEnvVars(context))
        .build();
  }

}
