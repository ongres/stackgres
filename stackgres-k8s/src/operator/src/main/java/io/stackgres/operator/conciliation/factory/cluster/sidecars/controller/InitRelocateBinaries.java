/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.controller;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_EXTENSIONS;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.SCRIPT_TEMPLATES;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresInitContainers;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContextUtil;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PostgresContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainers.RELOCATE_BINARIES)
public class InitRelocateBinaries implements ContainerFactory<StackGresClusterContainerContext> {

  private final VolumeMountsProvider<PostgresContainerContext> postgresExtensionsMounts;

  private final VolumeMountsProvider<ContainerContext> templateMounts;

  @Inject
  public InitRelocateBinaries(
      @ProviderName(POSTGRES_EXTENSIONS)
          VolumeMountsProvider<PostgresContainerContext> postgresExtensionsMounts,
      @ProviderName(SCRIPT_TEMPLATES)
          VolumeMountsProvider<ContainerContext> templateMounts) {
    this.postgresExtensionsMounts = postgresExtensionsMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final String patroniImageName = StackGresUtil.getPatroniImageName(clusterContext.getCluster());
    return new ContainerBuilder()
        .withName(StackGresInitContainers.RELOCATE_BINARIES.getName())
        .withImage(patroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH.filename())
        .withEnv(postgresExtensionsMounts.getDerivedEnvVars(ContextUtil.toPostgresContext(context)))
        .withVolumeMounts(templateMounts.getVolumeMounts(context))
        .addToVolumeMounts(new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterStatefulSetPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

}
