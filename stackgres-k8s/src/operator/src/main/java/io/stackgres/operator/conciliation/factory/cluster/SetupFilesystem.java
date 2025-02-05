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
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.SETUP_FILESYSTEM)
public class SetupFilesystem implements ContainerFactory<ClusterContainerContext> {

  private final PostgresExtensionMounts postgresExtensionsMounts;

  private final TemplatesMounts templateMounts;

  @Inject
  public SetupFilesystem(
      PostgresExtensionMounts postgresExtensionsMounts,
      TemplatesMounts templateMounts,
      UserOverrideMounts containerUserOverrideMounts) {
    this.postgresExtensionsMounts = postgresExtensionsMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final String patroniImageName = StackGresUtil.getPatroniImageName(clusterContext.getCluster());
    return new ContainerBuilder()
        .withName(StackGresInitContainer.SETUP_FILESYSTEM.getName())
        .withImage(patroniImageName)
        .withImagePullPolicy(getDefaultPullPolicy())
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_SETUP_FILESYSTEM_SH_PATH.filename())
        .addAllToEnv(postgresExtensionsMounts.getDerivedEnvVars(context))
        .addAllToEnv(templateMounts.getDerivedEnvVars(context))
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
            .withMountPath(ClusterPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

}
