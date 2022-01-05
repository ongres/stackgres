/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

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
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.FactoryName;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.distributedlogs.patroni.DistributedLogsEnvVarFactories;
import io.stackgres.operator.conciliation.factory.v09.PatroniStaticVolume;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@InitContainer(ClusterInitContainer.USER_SET_UP)
public class UserSetUp implements ContainerFactory<DistributedLogsContainerContext> {

  private final ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> commonEnvVarFactory;

  @Inject
  public UserSetUp(
      @FactoryName(DistributedLogsEnvVarFactories.V09_COMMON_ENV_VAR_FACTORY)
      ResourceFactory<StackGresDistributedLogsContext, List<EnvVar>> commonEnvVarFactory) {
    this.commonEnvVarFactory = commonEnvVarFactory;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage("busybox:1.31.1")
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(commonEnvVarFactory.createResource(context.getDistributedLogsContext()))
        .withVolumeMounts(
              new VolumeMountBuilder()
                  .withName(StatefulSetDynamicVolumes.SCRIPT_TEMPLATES.getVolumeName())
                  .withMountPath(ClusterStatefulSetPath.TEMPLATES_PATH.path())
                  .build(),
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.LOCAL.getVolumeName())
                .withMountPath("/local/etc")
                .withSubPath("etc")
                .build())
        .build();
  }

}
