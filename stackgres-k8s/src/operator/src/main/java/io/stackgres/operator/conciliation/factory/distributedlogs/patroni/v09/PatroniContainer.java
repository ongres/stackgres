/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;
import static io.stackgres.operator.conciliation.factory.distributedlogs.patroni.DistributedLogsEnvVarFactories.V09_PATRONI_ENV_VAR_FACTORY;

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
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.v09.PatroniStaticVolume;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@RunningContainer(ClusterRunningContainer.PATRONI_V09)
public class PatroniContainer implements ContainerFactory<DistributedLogsContainerContext> {

  private static final String IMAGE_NAME = "docker.io/ongres/patroni-ext:v1.6.5-pg12.6-build-6.0";

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final VolumeMountsProvider<ContainerContext> postgresDataMounts;
  private final VolumeMountsProvider<ContainerContext> postgresSocket;

  private final ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> envVarFactory;

  @Inject
  public PatroniContainer(
      @ProviderName(VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
      @ProviderName(VolumeMountProviderName.POSTGRES_DATA)
          VolumeMountsProvider<ContainerContext> postgresDataMounts,
      @ProviderName(VolumeMountProviderName.POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket,
      @FactoryName(V09_PATRONI_ENV_VAR_FACTORY)
          ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> envVarFactory) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresDataMounts = postgresDataMounts;
    this.postgresSocket = postgresSocket;
    this.envVarFactory = envVarFactory;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    final StackGresDistributedLogs distributedLogs =
        context.getDistributedLogsContext().getSource();
    final String startScript = "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(IMAGE_NAME)
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_PATH.path() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withVolumeMounts(getVolumeMounts(context))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(distributedLogs.getMetadata().getName()).build())
            .build())
        .withEnv(getEnvVars(context))
        .withPorts(
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_PORT).build(),
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/cluster")
                .withPort(new IntOrString(EnvoyUtil.PATRONI_ENTRY_PORT))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(15)
            .withPeriodSeconds(20)
            .withFailureThreshold(6)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/read-only")
                .withPort(new IntOrString(EnvoyUtil.PATRONI_ENTRY_PORT))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(10)
            .build())
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(DistributedLogsContainerContext context) {
    return Map.of();
  }

  public List<VolumeMount> getVolumeMounts(DistributedLogsContainerContext context) {
    final StackGresDistributedLogs distributedLogs = context.getDistributedLogsContext()
        .getSource();
    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresDataMounts.getVolumeMounts(context))
        .addAll(postgresSocket.getVolumeMounts(context))
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
                .build()
        ).addAll(containerUserOverrideMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.PATRONI_ENV.getVolumeName())
                .withMountPath("/etc/env/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.PATRONI_CONFIG.getVolumeName())
                .withMountPath("/etc/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(PatroniInitScriptConfigMap.name(distributedLogs))
                .withMountPath(
                    "/etc/patroni/init-script.d/00000-distributed-logs-template.template1.sql")
                .withSubPath("00000-distributed-logs-template.template1.sql")
                .withReadOnly(true)
                .build())
        .build();
  }

  public List<EnvVar> getEnvVars(DistributedLogsContainerContext context) {
    return envVarFactory.createResource(context.getDistributedLogsContext());
  }
}
