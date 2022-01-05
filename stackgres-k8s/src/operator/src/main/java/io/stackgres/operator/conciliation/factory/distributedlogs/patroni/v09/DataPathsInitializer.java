/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ClusterInitContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.DistributedLogsEnvVarFactories;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@InitContainer(ClusterInitContainer.DATA_PATHS_INITIALIZER)
public class DataPathsInitializer implements ContainerFactory<DistributedLogsContainerContext> {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> commonEnvVarFactory;

  @Inject
  public DataPathsInitializer(
      @ProviderName(CONTAINER_LOCAL_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
      @FactoryName(DistributedLogsEnvVarFactories.V09_COMMON_ENV_VAR_FACTORY)
          ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> commonEnvVarFactory) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.commonEnvVarFactory = commonEnvVarFactory;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage("busybox:1.31.1")
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename())
        .withEnv(commonEnvVarFactory.createResource(context.getDistributedLogsContext()))
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.SCRIPT_TEMPLATES.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.TEMPLATES_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_BASE_PATH.path())
                .build())
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .build();
  }

}
