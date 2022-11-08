/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

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
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariables;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.HugePagesMounts;
import io.stackgres.operator.conciliation.factory.distributedlogs.PostgresExtensionMounts;

@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.PATRONI)
public class Patroni implements ContainerFactory<DistributedLogsContainerContext> {

  private final ResourceFactory<StackGresDistributedLogsContext, ResourceRequirements>
      requirementsFactory;

  private final ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> envVarFactory;

  private final PostgresSocketMount postgresSocket;
  private final PostgresExtensionMounts postgresExtensions;
  private final LocalBinMounts localBinMounts;
  private final HugePagesMounts hugePagesMounts;

  @Inject
  public Patroni(
      ResourceFactory<StackGresDistributedLogsContext, ResourceRequirements>
          requirementsFactory,
      @FactoryName(DistributedLogsEnvVarFactories.LATEST_PATRONI_ENV_VAR_FACTORY)
      ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> envVarFactory,
      PostgresSocketMount postgresSocket,
      PostgresExtensionMounts postgresExtensions,
      LocalBinMounts localBinMounts,
      HugePagesMounts hugePagesMounts) {
    this.requirementsFactory = requirementsFactory;
    this.envVarFactory = envVarFactory;
    this.postgresSocket = postgresSocket;
    this.postgresExtensions = postgresExtensions;
    this.localBinMounts = localBinMounts;
    this.hugePagesMounts = hugePagesMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    final StackGresDistributedLogs cluster = context.getDistributedLogsContext().getSource();

    ResourceRequirements podResources = requirementsFactory
        .createResource(context.getDistributedLogsContext());

    return new ContainerBuilder()
        .withName(StackGresContainer.PATRONI.getName())
        .withImage(StackGresUtil.getPatroniImageName(cluster))
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_START_PATRONI_SH_PATH.path())
        .withResources(podResources)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(EnvoyUtil.POSTGRES_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_PORT).build(),
            new ContainerPortBuilder()
                .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
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
    return Map.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresDistributedLogsUtil
        .getPostgresVersion(context.getDistributedLogsContext().getSource()),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.get(context.getDistributedLogsContext().getSource())
        .getLatestVersion());
  }

  public List<VolumeMount> getVolumeMounts(DistributedLogsContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
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
        .add(
            new VolumeMountBuilder()
                .withName(StackGresVolume.PATRONI_ENV.getName())
                .withMountPath("/etc/env/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.PATRONI_CONFIG.getName())
                .withMountPath("/etc/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.INIT_SCRIPT.getName())
                .withMountPath("/etc/patroni/init-script.d/distributed-logs-template.template1.sql")
                .withSubPath("distributed-logs-template.sql")
                .withReadOnly(true)
                .build())
        .addAll(postgresExtensions.getVolumeMounts(context))
        .addAll(hugePagesMounts.getVolumeMounts(context))
        .build();
  }

  public List<EnvVar> getEnvVar(DistributedLogsContainerContext context) {

    final List<EnvVar> localBinMountsEnvVars = localBinMounts.getDerivedEnvVars(context);
    final List<EnvVar> postgresExtensionsEnvVars = postgresExtensions
        .getDerivedEnvVars(context);
    final List<EnvVar> resource = envVarFactory
        .createResource(context.getDistributedLogsContext());
    return ImmutableList.<EnvVar>builder()
        .addAll(localBinMountsEnvVars)
        .addAll(postgresExtensionsEnvVars)
        .addAll(resource)
        .add(
            ClusterStatefulSetPath.PATRONI_CONFIG_PATH.envVar(),
            ClusterStatefulSetPath.PATRONI_CONFIG_FILE_PATH.envVar(),
            ClusterStatefulSetPath.PATRONI_ENV_PATH.envVar(Map.of(
                ClusterStatefulSetEnvVars.PATRONI_ENV.name(),
                ClusterStatefulSetEnvVars.PATRONI_ENV.value())))
        .addAll(hugePagesMounts.getDerivedEnvVars(context))
        .build();
  }

}
