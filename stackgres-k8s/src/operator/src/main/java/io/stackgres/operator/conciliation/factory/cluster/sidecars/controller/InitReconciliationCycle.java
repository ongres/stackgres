/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.controller;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.CLUSTER_RECONCILIATION_CYCLE)
public class InitReconciliationCycle implements ContainerFactory<ClusterContainerContext> {

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainer.CLUSTER_RECONCILIATION_CYCLE.getName())
        .withImage(StackGresController.CLUSTER_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withEnv(new EnvVarBuilder()
                .withName("COMMAND")
                .withValue("run-reconciliation-cycle")
                .build(),
            new EnvVarBuilder()
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
                .withValue(Boolean.FALSE.toString())
                .build(),
            new EnvVarBuilder()
                .withName(ClusterControllerProperty
                    .CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER
                    .getEnvironmentVariableName())
                .withValue(Boolean.FALSE.toString())
                .build(),
            new EnvVarBuilder()
                .withName(ClusterControllerProperty
                    .CLUSTER_CONTROLLER_RECONCILE_PATRONI
                    .getEnvironmentVariableName())
                .withValue(Boolean.FALSE.toString())
                .build(),
            new EnvVarBuilder()
                .withName(ClusterControllerProperty
                    .CLUSTER_CONTROLLER_RECONCILE_POSTGRES_SSL
                    .getEnvironmentVariableName())
                .withValue(Boolean.FALSE.toString())
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
                .withName("DEBUG_CLUSTER_CONTROLLER")
                .withValue(System.getenv("DEBUG_OPERATOR"))
                .build(),
            new EnvVarBuilder()
                .withName("DEBUG_CLUSTER_CONTROLLER_SUSPEND")
                .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
                .build())
        .addToVolumeMounts(
            new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterStatefulSetPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

}
