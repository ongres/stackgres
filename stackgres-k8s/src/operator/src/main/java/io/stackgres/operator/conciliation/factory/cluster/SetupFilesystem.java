/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPathV2;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.RegistryBinding;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder(registry = RegistryBinding.ENABLED)
@InitContainer(StackGresInitContainer.SETUP_FILESYSTEM)
public class SetupFilesystem implements ContainerFactory<ClusterContainerContext> {

  private final PostgresExtensionMounts postgresExtensionsMounts;

  private final TemplatesMounts templateMounts;

  @Inject
  public SetupFilesystem(
      PostgresExtensionMounts postgresExtensionsMounts,
      TemplatesMounts templateMounts) {
    this.postgresExtensionsMounts = postgresExtensionsMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final String patroniImageName = clusterContext.getContext()
        .getMetadataManager()
        .getImage(clusterContext.getContext(), clusterContext.getCluster());
    return new ContainerBuilder()
        .withName(StackGresInitContainer.SETUP_FILESYSTEM.getName())
        .withImage(patroniImageName)
        .withImagePullPolicy(getDefaultPullPolicy())
        .withCommand("/bin/sh", "-ex",
            ClusterPathV2.TEMPLATES_PATH.path()
                + "/" + ClusterPathV2.LOCAL_BIN_SETUP_FILESYSTEM_SH_PATH.filename())
        .addAllToEnv(postgresExtensionsMounts.getDerivedEnvVars(context))
        .addAllToEnv(templateMounts.getDerivedEnvVars(context))
        .addToEnv(ClusterPathV2.PG_BASE_PATH.envVar())
        .addToEnv(new EnvVarBuilder()
            .withName("HOME")
            .withValue("/tmp")
            .build())
        .addAllToVolumeMounts(templateMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath("/local/etc")
            .withSubPath("etc")
            .build(),
            new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterPathV2.PG_BASE_PATH.path())
            .build())
        .build();
  }

}
