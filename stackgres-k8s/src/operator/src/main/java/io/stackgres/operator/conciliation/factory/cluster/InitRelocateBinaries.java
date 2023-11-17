/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.RELOCATE_BINARIES)
public class InitRelocateBinaries implements ContainerFactory<ClusterContainerContext> {

  private final PostgresExtensionMounts postgresExtensionsMounts;

  private final ScriptTemplatesVolumeMounts templateMounts;

  @Inject
  public InitRelocateBinaries(
      PostgresExtensionMounts postgresExtensionsMounts,
      ScriptTemplatesVolumeMounts templateMounts) {
    this.postgresExtensionsMounts = postgresExtensionsMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final String patroniImageName = StackGresUtil.getPatroniImageName(clusterContext.getCluster());
    return new ContainerBuilder()
        .withName(StackGresInitContainer.RELOCATE_BINARIES.getName())
        .withImage(patroniImageName)
        .withImagePullPolicy(getDefaultPullPolicy())
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH.filename())
        .withEnv(postgresExtensionsMounts.getDerivedEnvVars(context))
        .withVolumeMounts(templateMounts.getVolumeMounts(context))
        .addToVolumeMounts(new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

}
