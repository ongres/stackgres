/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.distributedlogs.controller;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.factory.ContainerResourceFactory;

@Singleton
public class DistributedLogsController implements ContainerResourceFactory<Void,
    StackGresDistributedLogsContext> {

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  @Inject
  public DistributedLogsController(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables) {
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
  }

  @Override
  public Container getContainer(StackGresDistributedLogsContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.DISTRIBUTEDLOGS_CONTROLLER)
        .withImage(StackGresController.DISTRIBUTEDLOGS_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withEnv(new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME
                .getEnvironmentVariableName())
            .withValue(context
                .getDistributedLogs().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE
                .getEnvironmentVariableName())
            .withValue(context
                .getDistributedLogs().getMetadata().getNamespace())
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
            .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER")
            .withValue(System.getenv("DEBUG_OPERATOR"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER_SUSPEND")
            .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
            .build())
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
            ClusterStatefulSetVolumeConfig.SOCKET.volumeMount(context),
            new VolumeMountBuilder()
            .withName(FluentdUtil.CONFIG)
            .withMountPath("/etc/fluentd")
            .withReadOnly(Boolean.TRUE)
            .build(),
            new VolumeMountBuilder()
            .withName(StackgresClusterContainers.FLUENTD)
            .withMountPath("/fluentd")
            .withReadOnly(Boolean.FALSE)
            .build())
        .build();
  }

  @Override
  public Stream<Container> getInitContainers(StackGresDistributedLogsContext context) {
    return Stream.of(
        relocateBinaries(context),
        reconciliationCycle(context));
  }

  private Container relocateBinaries(StackGresDistributedLogsContext context) {
    final String patroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            context.getCluster().getSpec().getPostgresVersion()));
    return new ContainerBuilder()
        .withName("relocate-binaries")
        .withImage(patroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

  private Container reconciliationCycle(StackGresDistributedLogsContext context) {
    return new ContainerBuilder()
        .withName("distributedlogs-reconciliation-cycle")
        .withImage(StackGresController.DISTRIBUTEDLOGS_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withEnv(new EnvVarBuilder()
            .withName("COMMAND")
            .withValue("run-reconciliation-cycle")
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME
                .getEnvironmentVariableName())
            .withValue(context
                .getDistributedLogs().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE
                .getEnvironmentVariableName())
            .withValue(context
                .getDistributedLogs().getMetadata().getNamespace())
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
            .withValue(Boolean.TRUE.toString())
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
            .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER")
            .withValue(System.getenv("DEBUG_OPERATOR"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER_SUSPEND")
            .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
            .build())
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
            ClusterStatefulSetVolumeConfig.SOCKET.volumeMount(context),
            new VolumeMountBuilder()
            .withName(FluentdUtil.CONFIG)
            .withMountPath("/etc/fluentd")
            .withReadOnly(Boolean.TRUE)
            .build(),
            new VolumeMountBuilder()
            .withName(StackgresClusterContainers.FLUENTD)
            .withMountPath("/fluentd")
            .withReadOnly(Boolean.FALSE)
            .build())
        .build();
  }

}
