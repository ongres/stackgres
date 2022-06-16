/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresInitContainers;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainers.SETUP_SCRIPTS)
public class ScriptsSetUp implements ContainerFactory<DistributedLogsContainerContext> {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;

  @Inject
  KubectlUtil kubectl;

  @Inject
  public ScriptsSetUp(
      @ProviderName(CONTAINER_USER_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainers.SETUP_SCRIPTS.getName())
        .withImage(kubectl
            .getImageName(context.getDistributedLogsContext().getSource()))
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(PatroniEnvPaths.getEnvVars())
        .addToEnv(new EnvVarBuilder().withName("HOME").withValue("/tmp").build())
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.SCRIPT_TEMPLATES.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.TEMPLATES_PATH.path())
                .build())
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.LOCAL_BIN.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_PATH.path())
                .build())
        .build();
  }

}
