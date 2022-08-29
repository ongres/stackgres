/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.PostgresExtensionMounts;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.RELOCATE_BINARIES)
public class InitRelocateBinaries implements ContainerFactory<DistributedLogsContainerContext> {

  private final ContainerUserOverrideMounts containerUserOverrideMounts;
  private final PostgresExtensionMounts postgresExtensionsMounts;
  private final ScriptTemplatesVolumeMounts templateMounts;

  @Inject
  public InitRelocateBinaries(
      ContainerUserOverrideMounts containerUserOverrideMounts,
      PostgresExtensionMounts postgresExtensionsMounts,
      ScriptTemplatesVolumeMounts templateMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresExtensionsMounts = postgresExtensionsMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainer.RELOCATE_BINARIES.getName())
        .withImage(StackGresUtil.getPatroniImageName(
            context.getDistributedLogsContext().getSource()))
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH.filename())
        .withEnv(getEnvVars(context))
        .withVolumeMounts(getVolumeMounts(context))
        .build();
  }

  public List<VolumeMount> getVolumeMounts(DistributedLogsContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
        .addAll(templateMounts.getVolumeMounts(context))
        .addAll(containerUserOverrideMounts.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterStatefulSetPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

  public List<EnvVar> getEnvVars(DistributedLogsContainerContext context) {
    return postgresExtensionsMounts.getDerivedEnvVars(context);
  }

}
