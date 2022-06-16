/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.LOCAL_BIN;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_EXTENSIONS;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_SOCKET;
import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainers;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContextUtil;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.PostgresContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;

@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainers.PATRONI)
public class PatroniContainer implements ContainerFactory<DistributedLogsContainerContext> {

  private final ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> envVarFactory;

  private final VolumeMountsProvider<ContainerContext> postgresSocket;
  private final VolumeMountsProvider<PostgresContainerContext> postgresExtensions;
  private final VolumeMountsProvider<ContainerContext> localBinMounts;

  @Inject
  public PatroniContainer(
      @FactoryName(DistributedLogsEnvVarFactories.LATEST_PATRONI_ENV_VAR_FACTORY)
          ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> envVarFactory,
      @ProviderName(POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket,
      @ProviderName(POSTGRES_EXTENSIONS)
          VolumeMountsProvider<PostgresContainerContext> postgresExtensions,
      @ProviderName(LOCAL_BIN)
          VolumeMountsProvider<ContainerContext> localBinMounts) {
    this.envVarFactory = envVarFactory;
    this.postgresSocket = postgresSocket;
    this.postgresExtensions = postgresExtensions;
    this.localBinMounts = localBinMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    final StackGresDistributedLogs cluster = context.getDistributedLogsContext().getSource();

    final String startScript = "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackGresContainers.PATRONI.getName())
        .withImage(StackGresUtil.getPatroniImageName(cluster))
        .withCommand("/bin/sh", "-ex",
            PatroniEnvPaths.LOCAL_BIN_PATH.getPath() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_PORT).build(),
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(getVolumeMounts(context))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(cluster.getMetadata().getName()).build())
            .build())
        .withEnv(getEnvVar(context))
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
    return ImmutableMap.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresDistributedLogsUtil
        .getPostgresVersion(context.getDistributedLogsContext().getSource()),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.get(context.getDistributedLogsContext().getSource())
        .findLatestVersion());
  }

  public List<VolumeMount> getVolumeMounts(DistributedLogsContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
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
                .build(),
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.INIT_SCRIPT.getVolumeName())
                .withMountPath("/etc/patroni/init-script.d/distributed-logs-template.template1.sql")
                .withSubPath("distributed-logs-template.sql")
                .withReadOnly(true)
                .build())
        .addAll(postgresExtensions.getVolumeMounts(ContextUtil.toPostgresContext(context)))
        .build();
  }

  public List<EnvVar> getEnvVar(DistributedLogsContainerContext context) {

    final List<EnvVar> localBinMountsEnvVars = localBinMounts.getDerivedEnvVars(context);
    final List<EnvVar> postgresExtensionsEnvVars = postgresExtensions
        .getDerivedEnvVars(ContextUtil.toPostgresContext(context));
    final List<EnvVar> resource = envVarFactory
        .createResource(context.getDistributedLogsContext());
    return ImmutableList.<EnvVar>builder()
        .addAll(localBinMountsEnvVars)
        .addAll(postgresExtensionsEnvVars)
        .addAll(resource)
        .add(new EnvVarBuilder()
                .withName("PATRONI_CONFIG_PATH")
                .withValue("/etc/patroni")
                .build(),
            new EnvVarBuilder()
                .withName("PATRONI_ENV_PATH")
                .withValue("/etc/env/patroni")
                .build())
        .build();
  }

}
