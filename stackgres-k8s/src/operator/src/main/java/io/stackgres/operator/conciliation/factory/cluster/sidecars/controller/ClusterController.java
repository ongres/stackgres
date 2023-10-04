/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloper;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperContainerPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;

@Singleton
@Sidecar(StackGresContainer.CLUSTER_CONTROLLER)
@OperatorVersionBinder
@RunningContainer(StackGresContainer.CLUSTER_CONTROLLER)
public class ClusterController implements ContainerFactory<ClusterContainerContext> {

  private final PostgresDataMounts postgresDataMounts;
  private final ContainerUserOverrideMounts userContainerMounts;
  private final PostgresSocketMount postgresSocket;

  @Inject
  public ClusterController(
      PostgresDataMounts postgresDataMounts,
      ContainerUserOverrideMounts userContainerMounts,
      PostgresSocketMount postgresSocket) {
    this.postgresDataMounts = postgresDataMounts;
    this.userContainerMounts = userContainerMounts;
    this.postgresSocket = postgresSocket;
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.CLUSTER_CONTROLLER.getName())
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
                    .map(StackGresClusterSpec::getPods)
                    .map(StackGresClusterPods::getDisableConnectionPooling)
                    .map(getDisableConnectionPooling -> !getDisableConnectionPooling)
                    .orElse(Boolean.TRUE)
                    .toString())
                .build(),
            new EnvVarBuilder()
                .withName(ClusterControllerProperty
                    .CLUSTER_CONTROLLER_RECONCILE_PATRONI
                    .getEnvironmentVariableName())
                .withValue(Boolean.TRUE.toString())
                .build(),
            new EnvVarBuilder()
                .withName(ClusterControllerProperty
                    .CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL
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
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.PGBOUNCER_CONFIG.getName())
                .withMountPath(ClusterPath.PGBOUNCER_CONFIG_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.PGBOUNCER.getName())
                .withMountPath(ClusterPath.PGBOUNCER_CONFIG_UPDATED_FILE_PATH.path())
                .withReadOnly(true)
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.PATRONI_CONFIG.getName())
                .withMountPath(ClusterPath.PATRONI_CONFIG_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.POSTGRES_SSL.getName())
                .withMountPath(ClusterPath.SSL_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.POSTGRES_SSL_COPY.getName())
                .withMountPath(ClusterPath.SSL_COPY_PATH.path())
                .build())
        .addAllToVolumeMounts(Optional.of(context.getClusterContext().getConfig().getSpec())
            .map(StackGresConfigSpec::getDeveloper)
            .map(StackGresConfigDeveloper::getPatches)
            .map(StackGresConfigDeveloperPatches::getClusterController)
            .map(StackGresConfigDeveloperContainerPatches::getVolumeMounts)
            .stream()
            .flatMap(List::stream)
            .map(VolumeMount.class::cast)
            .toList())
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.CLUSTER_CONTROLLER_VERSION_KEY,
        StackGresController.CLUSTER_CONTROLLER.getVersion());
  }

}
