/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractPatroniTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsStatefulSet;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@RunningContainer(order = 0)
public class PatroniContainer implements ContainerFactory<DistributedLogsContext> {

  static final String DISTRIBUTED_LOGS_TEMPLATE_NAME = "distributed-logs-template";
  private static final String SOCKET_VOLUME_NAME = "socket";
  private static final String LOCAL_VOLUME_NAME = "local";
  private static final String DSHM_VOLUME_NAME = "dshm";
  private static final String PATRONI_ENV_VOLUME_NAME = "patroni-env";
  private static final String PATRONI_CONFIG_VOLUME_NAME = "patroni-config";

  private final ResourceFactory<DistributedLogsContext, List<EnvVar>> envVarFactory;

  @Inject
  public PatroniContainer(ResourceFactory<DistributedLogsContext, List<EnvVar>> envVarFactory) {
    this.envVarFactory = envVarFactory;
  }

  @Override
  public Container getContainer(DistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String pgVersion = StackGresComponent.POSTGRESQL.findVersion(StackGresComponent.LATEST);

    final String patroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            pgVersion));

    final String startScript = "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            PatroniEnvPaths.LOCAL_BIN_PATH.getPath() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_PORT).build(),
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_PORT).build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(new VolumeMountBuilder()
                .withName(DistributedLogsStatefulSet.dataName(cluster))
                .withMountPath(PatroniEnvPaths.PG_BASE_PATH.getPath())
                .build(),
            new VolumeMountBuilder()
                .withName(SOCKET_VOLUME_NAME)
                .withMountPath(PatroniEnvPaths.PG_RUN_PATH.getPath())
                .build(),
            new VolumeMountBuilder()
                .withName(DSHM_VOLUME_NAME)
                .withMountPath(PatroniEnvPaths.SHARED_MEMORY_PATH.getPath())
                .build(),
            new VolumeMountBuilder()
                .withName(LOCAL_VOLUME_NAME)
                .withMountPath("/usr/local/bin")
                .withSubPath("usr/local/bin")
                .build(),
            new VolumeMountBuilder()
                .withName(LOCAL_VOLUME_NAME)
                .withMountPath("/var/log/postgresql")
                .withSubPath("var/log/postgresql")
                .build(),
            new VolumeMountBuilder()
                .withName(LOCAL_VOLUME_NAME)
                .withMountPath("/etc/passwd")
                .withSubPath("etc/passwd")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName(LOCAL_VOLUME_NAME)
                .withMountPath("/etc/group")
                .withSubPath("etc/group")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName(LOCAL_VOLUME_NAME)
                .withMountPath("/etc/shadow")
                .withSubPath("etc/shadow")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName(LOCAL_VOLUME_NAME)
                .withMountPath("/etc/gshadow")
                .withSubPath("etc/gshadow")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName(PATRONI_ENV_VOLUME_NAME)
                .withMountPath("/etc/env/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(PATRONI_CONFIG_VOLUME_NAME)
                .withMountPath("/etc/patroni")
                .build(),
            new VolumeMountBuilder()
                .withName(DISTRIBUTED_LOGS_TEMPLATE_NAME)
                .withMountPath("/etc/patroni/init-script.d/distributed-logs-template.template1.sql")
                .withSubPath("distributed-logs-template.sql")
                .withReadOnly(true)
                .build())
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(cluster.getMetadata().getName()).build())
            .build())
        .withEnv(envVarFactory.createResource(context))
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withNewPath("/cluster")
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
  public Map<String, String> getComponentVersions(DistributedLogsContext context) {
    return ImmutableMap.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresComponent.POSTGRESQL.findVersion(StackGresComponent.LATEST),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.findLatestVersion());
  }

  @Override
  public List<Volume> getVolumes(DistributedLogsContext context) {

    final StackGresDistributedLogs source = context.getSource();

    return List.of(
        new VolumeBuilder()
            .withName(SOCKET_VOLUME_NAME)
            .withNewEmptyDir()
            .withMedium("Memory")
            .endEmptyDir()
            .build(),
        new VolumeBuilder()
            .withName(DSHM_VOLUME_NAME)
            .withNewEmptyDir()
            .withMedium("Memory")
            .endEmptyDir()
            .build(),
        new VolumeBuilder()
            .withName("shared")
            .withNewEmptyDir()
            .endEmptyDir().build(),
        new VolumeBuilder()
            .withName(LOCAL_VOLUME_NAME)
            .withNewEmptyDir()
            .endEmptyDir()
            .build(),
        new VolumeBuilder()
            .withName(PATRONI_ENV_VOLUME_NAME)
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withDefaultMode(444)
                .withName(source.getMetadata().getName())
                .build())
            .build(),
        new VolumeBuilder()
            .withName(PATRONI_CONFIG_VOLUME_NAME)
            .withNewEmptyDir()
            .endEmptyDir()
            .build(),
        new VolumeBuilder()
            .withName("distributed-logs-templates")
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(AbstractPatroniTemplatesConfigMap.name(source))
                .withDefaultMode(444)
                .withOptional(false)
                .build())
            .build(),
        new VolumeBuilder()
            .withName(DISTRIBUTED_LOGS_TEMPLATE_NAME)
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(PatroniInitScriptConfigMap.name(source))
                .withDefaultMode(420)
                .withOptional(false)
                .build())
            .build());
  }
}
