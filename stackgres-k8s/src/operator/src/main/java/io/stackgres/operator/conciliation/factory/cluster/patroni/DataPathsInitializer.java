/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_DATA;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.SCRIPT_TEMPLATES;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresInitContainers;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainers.SETUP_DATA_PATHS)
public class DataPathsInitializer implements ContainerFactory<StackGresClusterContainerContext> {

  private final VolumeMountsProvider<ContainerContext> postgresDataMounts;

  private final VolumeMountsProvider<ContainerContext> scriptTemplateMounts;

  private final VolumeMountsProvider<ContainerContext> containerUserOverride;

  @Inject
  KubectlUtil kubectl;

  @Inject
  public DataPathsInitializer(
      @ProviderName(POSTGRES_DATA)
          VolumeMountsProvider<ContainerContext> postgresDataMounts,
      @ProviderName(SCRIPT_TEMPLATES)
          VolumeMountsProvider<ContainerContext> scriptTemplateMounts,
      @ProviderName(CONTAINER_USER_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverride) {
    this.postgresDataMounts = postgresDataMounts;
    this.scriptTemplateMounts = scriptTemplateMounts;
    this.containerUserOverride = containerUserOverride;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainers.SETUP_DATA_PATHS.getName())
        .withImage(kubectl.getImageName(context.getClusterContext().getCluster()))
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename())
        .withEnv(getClusterEnvVars(context))
        .addToEnv(new EnvVarBuilder().withName("HOME").withValue("/tmp").build())
        .withVolumeMounts(containerUserOverride.getVolumeMounts(context))
        .addAllToVolumeMounts(scriptTemplateMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(postgresDataMounts.getVolumeMounts(context))
        .build();
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContainerContext context) {
    return postgresDataMounts.getDerivedEnvVars(context);
  }

}
