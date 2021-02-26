/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.controller;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;

@Singleton
@Sidecar(ClusterController.NAME)
public class ClusterController implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String NAME = StackgresClusterContainers.CLUSTER_CONTROLLER;

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  @Inject
  public ClusterController(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables) {
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.CLUSTER_CONTROLLER)
        .withImage(StackGresController.CLUSTER_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withEnv(new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_NAME.getEnvironmentVariableName())
            .withValue(context
                .getCluster().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_NAMESPACE.getEnvironmentVariableName())
            .withValue(context
                .getCluster().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME
                .getEnvironmentVariableName())
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_CONTROLLER_EXTENSIONS_REPOSITORY_URLS
                .getEnvironmentVariableName())
            .withValue(OperatorProperty.EXTENSIONS_REPOSITORY_URLS
                .getString())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty
                .CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES
                .getEnvironmentVariableName())
            .withValue(Boolean.TRUE.toString())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CONTROLLER_LOG_LEVEL")
            .withValue(System.getenv("OPERATOR_LOG_LEVEL"))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CONTROLLER_SHOW_STACK_TRACES")
            .withValue(System.getenv("OPERATOR_SHOW_STACK_TRACES"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_CLUSTER_CONTROLLER")
            .withValue(System.getenv("DEBUG_OPERATOR"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_CLUSTER_CONTROLLER_SUSPEND")
            .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

  @Override
  public Stream<Container> getInitContainers(StackGresClusterContext context) {
    return Stream.of(
        relocateBinaries(context),
        mockBinaries(context),
        reconciliationCycle(context));
  }

  private Container relocateBinaries(StackGresClusterContext context) {
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

  private Container mockBinaries(StackGresClusterContext config) {
    final String patroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            config.getCluster().getSpec().getPostgresVersion()));
    return new ContainerBuilder()
        .withName("mock-binaries")
        .withImage(patroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_MOCK_BINARIES_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(config))
        .build();
  }

  private Container reconciliationCycle(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("cluster-reconciliation-cycle")
        .withImage(StackGresController.CLUSTER_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withEnv(new EnvVarBuilder()
            .withName("COMMAND")
            .withValue("run-reconciliation-cycle")
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_NAME.getEnvironmentVariableName())
            .withValue(context
                .getCluster().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_NAMESPACE.getEnvironmentVariableName())
            .withValue(context
                .getCluster().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME
                .getEnvironmentVariableName())
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_CONTROLLER_EXTENSIONS_REPOSITORY_URLS
                .getEnvironmentVariableName())
            .withValue(OperatorProperty.EXTENSIONS_REPOSITORY_URLS
                .getString())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty
                .CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES
                .getEnvironmentVariableName())
            .withValue(Boolean.FALSE.toString())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CONTROLLER_LOG_LEVEL")
            .withValue(System.getenv("OPERATOR_LOG_LEVEL"))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CONTROLLER_SHOW_STACK_TRACES")
            .withValue(System.getenv("OPERATOR_SHOW_STACK_TRACES"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_CLUSTER_CONTROLLER")
            .withValue(System.getenv("DEBUG_OPERATOR"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_CLUSTER_CONTROLLER_SUSPEND")
            .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

}
