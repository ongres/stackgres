/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.controller;

import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.PatroniEnvPaths;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@RunningContainer(order = 3)
public class DistributedLogsController implements ContainerFactory<DistributedLogsContext> {

  public static final String IMAGE_NAME = "docker.io/stackgres/distributedlogs-controller:%s";

  @Override
  public Container getContainer(DistributedLogsContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.DISTRIBUTEDLOGS_CONTROLLER)
        .withImage(StackGresController.DISTRIBUTEDLOGS_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/health/live")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(10)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/health/ready")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(2)
            .build())
        .withEnv(new EnvVarBuilder()
                .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME
                    .getEnvironmentVariableName())
                .withValue(context
                    .getSource().getMetadata().getName())
                .build(),
            new EnvVarBuilder()
                .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE
                    .getEnvironmentVariableName())
                .withValue(context
                    .getSource().getMetadata().getNamespace())
                .build(),
            new EnvVarBuilder()
                .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME
                    .getEnvironmentVariableName())
                .withValueFrom(new EnvVarSourceBuilder()
                    .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                    .build())
                .build(),
            new EnvVarBuilder()
                .withName(DistributedLogsControllerProperty
                    .DISTRIBUTEDLOGS_CONTROLLER_EXTENSIONS_REPOSITORY_URLS
                    .getEnvironmentVariableName())
                .withValue(OperatorProperty.EXTENSIONS_REPOSITORY_URLS
                    .getString())
                .build(),
            new EnvVarBuilder()
                .withName(DistributedLogsControllerProperty
                    .DISTRIBUTEDLOGS_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES
                    .getEnvironmentVariableName())
                .withValue(Boolean.FALSE.toString())
                .build(),
            new EnvVarBuilder()
                .withName("DISTRIBUTEDLOGS_CONTROLLER_LOG_LEVEL")
                .withValue(System.getenv("OPERATOR_LOG_LEVEL"))
                .build(),
            new EnvVarBuilder()
                .withName("DISTRIBUTEDLOGS_CONTROLLER_SHOW_STACK_TRACES")
                .withValue(System.getenv("OPERATOR_SHOW_STACK_TRACES"))
                .build(),
            new EnvVarBuilder()
                .withName("APP_OPTS")
                .withValue(System.getenv("APP_OPTS"))
                .build(),
            new EnvVarBuilder()
                .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER")
                .withValue(System.getenv("DEBUG_OPERATOR"))
                .build(),
            new EnvVarBuilder()
                .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER_SUSPEND")
                .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
                .build())
        .withVolumeMounts(new VolumeMountBuilder()
                .withName("socket")
                .withMountPath(PatroniEnvPaths.PG_RUN_PATH.getPath())
                .build(),
            new VolumeMountBuilder()
                .withName(FluentdUtil.CONFIG)
                .withMountPath("/etc/fluentd")
                .withReadOnly(Boolean.TRUE)
                .build(),
            new VolumeMountBuilder()
                .withName(FluentdUtil.NAME)
                .withMountPath("/fluentd")
                .withReadOnly(Boolean.FALSE)
                .build(),
            new VolumeMountBuilder()
                .withName("local")
                .withMountPath("/etc/passwd")
                .withSubPath("etc/passwd")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName("local")
                .withMountPath("/etc/group")
                .withSubPath("etc/group")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName("local")
                .withMountPath("/etc/shadow")
                .withSubPath("etc/shadow")
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName("local")
                .withMountPath("/etc/gshadow")
                .withSubPath("etc/gshadow")
                .withReadOnly(true)
                .build())
        .build();
  }

  @Override
  public List<Volume> getVolumes(DistributedLogsContext context) {
    return List.of();
  }

  @Override
  public Map<String, String> getComponentVersions(DistributedLogsContext context) {
    return ImmutableMap.of(
        StackGresContext.DISTRIBUTEDLOGS_CONTROLLER_VERSION_KEY,
        StackGresController.DISTRIBUTEDLOGS_CONTROLLER.getVersion());
  }

}
