/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.cluster.BackupVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.HugePagesMounts;
import io.stackgres.operator.conciliation.factory.cluster.PostgresExtensionMounts;
import io.stackgres.operator.conciliation.factory.cluster.ReplicateVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.RestoreVolumeMounts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_4)
@RunningContainer(StackGresContainer.PATRONI)
public class Patroni implements ContainerFactory<ClusterContainerContext> {

  private final ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;

  private final ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory;
  private final PostgresSocketMount postgresSocket;
  private final PostgresExtensionMounts postgresExtensions;
  private final LocalBinMounts localBinMounts;
  private final RestoreVolumeMounts restoreMounts;
  private final BackupVolumeMounts backupMounts;
  private final ReplicateVolumeMounts replicateMounts;
  private final PatroniVolumeMounts patroniMounts;
  private final HugePagesMounts hugePagesMounts;
  private final VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;

  @Inject
  public Patroni(
      ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables,
      ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory,
      PostgresSocketMount postgresSocket,
      PostgresExtensionMounts postgresExtensions,
      LocalBinMounts localBinMounts,
      RestoreVolumeMounts restoreMounts,
      BackupVolumeMounts backupMounts,
      ReplicateVolumeMounts replicateMounts,
      PatroniVolumeMounts patroniMounts,
      HugePagesMounts hugePagesMounts,
      VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer) {
    super();
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.requirementsFactory = requirementsFactory;
    this.postgresSocket = postgresSocket;
    this.postgresExtensions = postgresExtensions;
    this.localBinMounts = localBinMounts;
    this.restoreMounts = restoreMounts;
    this.backupMounts = backupMounts;
    this.replicateMounts = replicateMounts;
    this.patroniMounts = patroniMounts;
    this.hugePagesMounts = hugePagesMounts;
    this.volumeDiscoverer = volumeDiscoverer;
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresComponent.POSTGRESQL.get(context.getClusterContext().getCluster())
        .getVersion(
            context.getClusterContext().getCluster().getSpec().getPostgres().getVersion()),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.get(context.getClusterContext().getCluster())
        .getLatestVersion());
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getSource();
    final String patroniImageName = StackGresUtil.getPatroniImageName(cluster);

    ResourceRequirements podResources = requirementsFactory
        .createResource(clusterContext);

    ImmutableList.Builder<VolumeMount> volumeMounts = ImmutableList.<VolumeMount>builder()
        .addAll(postgresSocket.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(StackGresVolume.DSHM.getName())
            .withMountPath(ClusterStatefulSetPath.SHARED_MEMORY_PATH.path())
            .build())
        .add(new VolumeMountBuilder()
            .withName(StackGresVolume.LOG.getName())
            .withMountPath(ClusterStatefulSetPath.PG_LOG_PATH.path())
            .build())
        .addAll(localBinMounts.getVolumeMounts(context))
        .addAll(patroniMounts.getVolumeMounts(context))
        .addAll(backupMounts.getVolumeMounts(context))
        .addAll(replicateMounts.getVolumeMounts(context))
        .addAll(postgresExtensions.getVolumeMounts(context))
        .addAll(hugePagesMounts.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(StackGresVolume.POSTGRES_SSL_COPY.getName())
            .withMountPath(ClusterStatefulSetPath.SSL_PATH.path())
            .withReadOnly(true)
            .build());

    Optional.ofNullable(cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName).ifPresent(ignore ->
            volumeMounts.addAll(restoreMounts.getVolumeMounts(context))
        );

    return new ContainerBuilder()
        .withName(StackGresContainer.PATRONI.getName())
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_SH_PATH.path())
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT)
                .build(),
            new ContainerPortBuilder()
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT)
                .build(),
            new ContainerPortBuilder()
                .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(volumeMounts.build())
        .withEnv(getEnvVars(context))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(clusterContext)).build())
            .build())
        .addToEnv(new EnvVarBuilder()
            .withName("PATRONI_CONFIG_MD5SUM")
            .withValue(volumeDiscoverer.discoverVolumes(clusterContext)
                .get(StackGresVolume.PATRONI_ENV.getName())
                .getSource()
                .map(ConfigMap.class::cast)
                .map(ConfigMap::getData)
                .map(data -> data.get(StackGresUtil.MD5SUM_KEY))
                .orElseThrow())
            .build())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/liveness")
                .withPort(new IntOrString(EnvoyUtil.PATRONI_ENTRY_PORT))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(15)
            .withPeriodSeconds(20)
            .withFailureThreshold(6)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/readiness")
                .withPort(new IntOrString(EnvoyUtil.PATRONI_ENTRY_PORT))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(0)
            .withPeriodSeconds(2)
            .withTimeoutSeconds(1)
            .build())
        .withResources(podResources)
        .build();
  }

  private ImmutableList<EnvVar> getEnvVars(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    return ImmutableList.<EnvVar>builder()
        .addAll(localBinMounts.getDerivedEnvVars(context))
        .addAll(postgresExtensions.getDerivedEnvVars(context))
        .addAll(patroniEnvironmentVariables.createResource(clusterContext))
        .addAll(patroniMounts.getDerivedEnvVars(context))
        .addAll(backupMounts.getDerivedEnvVars(context))
        .addAll(replicateMounts.getDerivedEnvVars(context))
        .addAll(restoreMounts.getDerivedEnvVars(context))
        .addAll(hugePagesMounts.getDerivedEnvVars(context))
        .build();
  }

}
