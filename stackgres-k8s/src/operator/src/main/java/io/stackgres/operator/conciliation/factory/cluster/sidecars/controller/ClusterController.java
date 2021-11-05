/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.controller;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_DATA;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;

@Singleton
@Sidecar(ClusterController.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V11)
@RunningContainer(ClusterRunningContainer.CLUSTER_CONTROLLER)
public class ClusterController implements ContainerFactory<StackGresClusterContainerContext> {

  public static final String NAME = StackgresClusterContainers.CLUSTER_CONTROLLER;

  private final VolumeMountsProvider<ContainerContext> postgresDataMounts;
  private final VolumeMountsProvider<ContainerContext> userContainerMounts;

  @Inject
  public ClusterController(
      @ProviderName(POSTGRES_DATA)
          VolumeMountsProvider<ContainerContext> postgresDataMounts,
      @ProviderName(CONTAINER_USER_OVERRIDE)
      VolumeMountsProvider<ContainerContext> userContainerMounts) {
    this.postgresDataMounts = postgresDataMounts;
    this.userContainerMounts = userContainerMounts;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.CLUSTER_CONTROLLER)
        .withImage(StackGresController.CLUSTER_CONTROLLER.getImageName())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withEnv(new EnvVarBuilder()
                .withName(ClusterControllerProperty.CLUSTER_NAME.getEnvironmentVariableName())
                .withValue(context
                    .getClusterContext()
                    .getCluster().getMetadata().getName())
                .build(),
            new EnvVarBuilder()
                .withName(ClusterControllerProperty.CLUSTER_NAMESPACE.getEnvironmentVariableName())
                .withValue(context
                    .getClusterContext()
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
                .withName(ClusterControllerProperty
                    .CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER
                    .getEnvironmentVariableName())
                .withValue(Optional.of(context.getClusterContext().getCluster())
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getPod)
                    .map(StackGresClusterPod::getDisableConnectionPooling)
                    .map(getDisableConnectionPooling -> !getDisableConnectionPooling)
                    .orElse(Boolean.TRUE)
                    .toString())
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
                .withName("APP_OPTS")
                .withValue(System.getenv("APP_OPTS"))
                .build(),
            new EnvVarBuilder()
                .withName("JAVA_OPTS")
                .withValue(System.getenv("JAVA_OPTS"))
                .build(),
            new EnvVarBuilder()
                .withName("DEBUG_CLUSTER_CONTROLLER")
                .withValue(System.getenv("DEBUG_OPERATOR"))
                .build(),
            new EnvVarBuilder()
                .withName("DEBUG_CLUSTER_CONTROLLER_SUSPEND")
                .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
                .build())
        .withVolumeMounts(userContainerMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(postgresDataMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_AUTH_FILE.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.path())
            .withSubPath(ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.subPath())
            .withReadOnly(false)
            .build())
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.CLUSTER_CONTROLLER_VERSION_KEY,
        StackGresController.CLUSTER_CONTROLLER.getVersion());
  }

}
