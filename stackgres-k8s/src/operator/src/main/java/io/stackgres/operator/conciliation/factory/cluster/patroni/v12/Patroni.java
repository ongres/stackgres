/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v12;

import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContextUtil;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.PostgresContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniDefaultScripts;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_0, stopAt = StackGresVersion.V_1_2)
@RunningContainer(ClusterRunningContainer.PATRONI)
public class Patroni implements ContainerFactory<StackGresClusterContainerContext> {

  private final ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;

  private final ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory;
  private final VolumeMountsProvider<ContainerContext> postgresSocket;
  private final VolumeMountsProvider<PostgresContainerContext> postgresExtensions;
  private final VolumeMountsProvider<ContainerContext> localBinMounts;
  private final VolumeMountsProvider<ContainerContext> restoreMounts;
  private final VolumeMountsProvider<ContainerContext> backupMounts;
  private final VolumeMountsProvider<StackGresClusterContainerContext> hugePagesMounts;
  private final VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;
  private final PatroniDefaultScripts patroniDefaultScripts;

  @Inject
  public Patroni(
      ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables,
      ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory,
      @ProviderName(VolumeMountProviderName.POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket,
      @ProviderName(VolumeMountProviderName.POSTGRES_EXTENSIONS)
          VolumeMountsProvider<PostgresContainerContext> postgresExtensions,
      @ProviderName(VolumeMountProviderName.LOCAL_BIN)
          VolumeMountsProvider<ContainerContext> localBinMounts,
      @ProviderName(VolumeMountProviderName.RESTORE)
          VolumeMountsProvider<ContainerContext> restoreMounts,
      @ProviderName(VolumeMountProviderName.BACKUP)
          VolumeMountsProvider<ContainerContext> backupMounts,
      @ProviderName(VolumeMountProviderName.HUGE_PAGES)
          VolumeMountsProvider<StackGresClusterContainerContext> hugePagesMounts,
      VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer,
      PatroniDefaultScripts patroniDefaultScripts) {
    super();
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.requirementsFactory = requirementsFactory;
    this.postgresSocket = postgresSocket;
    this.postgresExtensions = postgresExtensions;
    this.localBinMounts = localBinMounts;
    this.restoreMounts = restoreMounts;
    this.backupMounts = backupMounts;
    this.hugePagesMounts = hugePagesMounts;
    this.volumeDiscoverer = volumeDiscoverer;
    this.patroniDefaultScripts = patroniDefaultScripts;
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresComponent.POSTGRESQL.get(context.getClusterContext().getCluster())
        .findVersion(
            context.getClusterContext().getCluster().getSpec().getPostgres().getVersion()),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.get(context.getClusterContext().getCluster())
        .findLatestVersion());
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getSource();
    final String patroniImageName = StackGresUtil.getPatroniImageName(cluster);

    ResourceRequirements podResources = requirementsFactory
        .createResource(clusterContext);

    final String startScript = Optional
        .ofNullable(cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName).isPresent()
        ? "/start-patroni-with-restore.sh" : "/start-patroni.sh";

    final PostgresContainerContext postgresContext = ContextUtil.toPostgresContext(context);

    ImmutableList.Builder<VolumeMount> volumeMounts = ImmutableList.<VolumeMount>builder()
        .addAll(postgresSocket.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(PatroniStaticVolume.DSHM.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.SHARED_MEMORY_PATH.path())
            .build())
        .add(new VolumeMountBuilder()
            .withName(PatroniStaticVolume.LOG.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.PG_LOG_PATH.path())
            .build())
        .addAll(localBinMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.PATRONI_ENV.getVolumeName())
                .withMountPath("/etc/env/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.PATRONI_CONFIG.getVolumeName())
                .withMountPath("/etc/patroni")
                .build())
        .addAll(backupMounts.getVolumeMounts(context))
        .addAll(postgresExtensions.getVolumeMounts(postgresContext))
        .addAll(hugePagesMounts.getVolumeMounts(context));

    Optional.ofNullable(cluster.getSpec().getInitData())
        .map(StackGresClusterInitData::getRestore)
        .map(StackGresClusterRestore::getFromBackup)
        .map(StackGresClusterRestoreFromBackup::getName).ifPresent(ignore ->
            volumeMounts.addAll(restoreMounts.getVolumeMounts(context))
        );

    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_PATH.path() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(volumeMounts.build())
        .addToVolumeMounts(
            patroniDefaultScripts.getIndexedScripts(cluster).stream()
                .map(t -> new VolumeMountBuilder()
                    .withName(PatroniScriptsConfigMap.name(clusterContext, t))
                    .withMountPath("/etc/patroni/init-script.d/"
                        + PatroniScriptsConfigMap.scriptName(t))
                    .withSubPath(t.v1.getScript() != null
                        ? PatroniScriptsConfigMap.scriptName(t)
                        : t.v1.getScriptFrom().getConfigMapKeyRef() != null
                        ? t.v1.getScriptFrom().getConfigMapKeyRef().getKey()
                        : t.v1.getScriptFrom().getSecretKeyRef().getKey())
                    .withReadOnly(true)
                    .build())
                .toArray(VolumeMount[]::new))
        .withEnv(getEnvVars(context))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(clusterContext)).build())
            .build())
        .addToEnv(new EnvVarBuilder()
            .withName("PATRONI_CONFIG_MD5SUM")
            .withValue(volumeDiscoverer.discoverVolumes(clusterContext)
                .entrySet()
                .stream()
                .filter(volumePairEntry -> volumePairEntry.getKey().equals(
                    StatefulSetDynamicVolumes.PATRONI_ENV.getVolumeName()))
                .map(Map.Entry::getValue)
                .map(VolumePair::getSource)
                .map(Optional::get)
                .map(ConfigMap.class::cast)
                .map(ConfigMap::getData)
                .map(data -> data.get(StackGresUtil.MD5SUM_KEY))
                .findFirst()
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
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(10)
            .build())
        .withResources(podResources)
        .build();
  }

  private ImmutableList<EnvVar> getEnvVars(StackGresClusterContainerContext context) {
    return ImmutableList.<EnvVar>builder()
        .addAll(localBinMounts.getDerivedEnvVars(context))
        .addAll(postgresExtensions.getDerivedEnvVars(ContextUtil.toPostgresContext(context)))
        .addAll(patroniEnvironmentVariables.createResource(context.getClusterContext()))
        .add(new EnvVarBuilder()
                .withName("PATRONI_CONFIG_PATH")
                .withValue("/etc/patroni")
                .build(),
            new EnvVarBuilder()
                .withName("PATRONI_ENV_PATH")
                .withValue("/etc/env/patroni")
                .build())
        .addAll(backupMounts.getDerivedEnvVars(context))
        .addAll(restoreMounts.getDerivedEnvVars(context))
        .addAll(hugePagesMounts.getDerivedEnvVars(context))
        .build();
  }

}
