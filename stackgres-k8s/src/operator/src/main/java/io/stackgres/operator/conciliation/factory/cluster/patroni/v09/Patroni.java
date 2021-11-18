/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.PatroniScriptsConfigMap;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactoryDiscoverer;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import io.stackgres.operator.conciliation.factory.v09.PatroniStaticVolume;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@RunningContainer(ClusterRunningContainer.PATRONI_V09)
public class Patroni implements ContainerFactory<StackGresClusterContainerContext> {

  public static final String POST_INIT_SUFFIX = "-post-init";

  private static final String IMAGE_NAME = "docker.io/ongres/patroni-ext:v1.6.5-pg%s-build-6.0";

  private final ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext>
      clusterEnvVarFactoryDiscoverer;
  private final ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;

  private final ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;

  private final VolumeMountsProvider<ContainerContext> postgresDataMounts;

  private final VolumeMountsProvider<ContainerContext> postgresSocket;

  private final VolumeMountsProvider<ContainerContext> restoreMounts;

  private final VolumeMountsProvider<ContainerContext> backupMounts;

  @Inject
  public Patroni(
      ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext>
          clusterEnvVarFactoryDiscoverer,
      ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables,
      ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      @ProviderName(VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts,
      @ProviderName(VolumeMountProviderName.POSTGRES_DATA)
          VolumeMountsProvider<ContainerContext> postgresDataMounts,
      @ProviderName(VolumeMountProviderName.POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket,
      @ProviderName(VolumeMountProviderName.RESTORE)
          VolumeMountsProvider<ContainerContext> restoreMounts,
      @ProviderName(VolumeMountProviderName.BACKUP)
          VolumeMountsProvider<ContainerContext> backupMounts) {
    super();
    this.clusterEnvVarFactoryDiscoverer = clusterEnvVarFactoryDiscoverer;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.requirementsFactory = requirementsFactory;
    this.labelFactory = labelFactory;
    this.containerUserOverrideMounts = containerLocalOverrideMounts;
    this.postgresDataMounts = postgresDataMounts;
    this.postgresSocket = postgresSocket;
    this.restoreMounts = restoreMounts;
    this.backupMounts = backupMounts;
  }

  public String postInitName(StackGresClusterContext clusterContext) {
    final StackGresCluster cluster = clusterContext.getSource();
    final String clusterName = labelFactory.resourceName(cluster);
    return clusterName + POST_INIT_SUFFIX;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getSource();

    final String patroniImageName = String.format(IMAGE_NAME,
        cluster.getSpec().getPostgres().getVersion());

    ResourceRequirements podResources = requirementsFactory
        .createResource(clusterContext);

    final String startScript = clusterContext.getRestoreBackup().isPresent()
        ? "/start-patroni-with-restore.sh" : "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_PATH.path() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(getVolumeMounts(context))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(clusterContext)).build())
            .build())
        .withEnv(ImmutableList.<EnvVar>builder()
            .addAll(getClusterEnvVars(clusterContext))
            .addAll(patroniEnvironmentVariables.createResource(clusterContext))
            .build())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/cluster")
                .withPort(new IntOrString(8008))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(15)
            .withPeriodSeconds(20)
            .withFailureThreshold(6)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/read-only")
                .withPort(new IntOrString(8008))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(10)
            .build())
        .withResources(podResources)
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return Map.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresComponent.POSTGRESQL.findVersion(
            context.getClusterContext().getCluster().getSpec().getPostgres().getVersion()),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.findLatestVersion());
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ClusterEnvironmentVariablesFactory<ClusterContext>> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(envVarFactory ->
        clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }

  private List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    ImmutableList.Builder<VolumeMount> volumeMounts = ImmutableList.builder();

    volumeMounts.addAll(postgresDataMounts.getVolumeMounts(context));
    volumeMounts.addAll(postgresSocket.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.DSHM.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.SHARED_MEMORY_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.LOCAL.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_PATH.path())
                .withSubPath("usr/local/bin")
                .build(),
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.LOCAL.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_LOG_PATH.path())
                .withSubPath("var/log/postgresql")
                .build())
        .addAll(containerUserOverrideMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.PATRONI_ENV.getVolumeName())
                .withMountPath("/etc/env/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.PATRONI_CONFIG.getVolumeName())
                .withMountPath("/etc/patroni")
                .build())
        .addAll(backupMounts.getVolumeMounts(context));

    if (context.getClusterContext().getRestoreBackup().isPresent()) {
      volumeMounts.addAll(restoreMounts.getVolumeMounts(context));
    }

    var clusterContext = context.getClusterContext();
    volumeMounts.add(clusterContext.getIndexedScripts().stream()
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
        .toArray(VolumeMount[]::new));

    return volumeMounts.build();
  }

}
