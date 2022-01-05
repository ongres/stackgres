/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterInitContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactoryDiscoverer;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@InitContainer(ClusterInitContainer.DATA_PATHS_INITIALIZER)
public class DataPathsInitializer implements ContainerFactory<StackGresClusterContainerContext> {

  private final ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext>
      clusterEnvVarFactoryDiscoverer;

  private final VolumeMountsProvider<ContainerContext> containerLocalOverride;

  @Inject
  public DataPathsInitializer(
      ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext>
          clusterEnvVarFactoryDiscoverer,
      @ProviderName(VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerLocalOverride) {
    this.clusterEnvVarFactoryDiscoverer = clusterEnvVarFactoryDiscoverer;
    this.containerLocalOverride = containerLocalOverride;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage("busybox:1.31.1")
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            io.stackgres.common.ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename())
        .withEnv(getClusterEnvVars(context.getClusterContext()))
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.SCRIPT_TEMPLATES.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.TEMPLATES_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_BASE_PATH.path())
                .build())
        .addAllToVolumeMounts(containerLocalOverride.getVolumeMounts(context))
        .build();
  }

  @NotNull
  private List<EnvVar> getClusterEnvVars(StackGresClusterContext context) {
    List<EnvVar> clusterEnvVars = new ArrayList<>();

    List<ClusterEnvironmentVariablesFactory<ClusterContext>> clusterEnvVarFactories =
        clusterEnvVarFactoryDiscoverer.discoverFactories(context);

    clusterEnvVarFactories.forEach(envVarFactory ->
        clusterEnvVars.addAll(envVarFactory.buildEnvironmentVariables(context)));
    return clusterEnvVars;
  }
}
