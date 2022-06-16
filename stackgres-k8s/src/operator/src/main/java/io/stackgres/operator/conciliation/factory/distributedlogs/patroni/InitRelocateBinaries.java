/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_EXTENSIONS;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.SCRIPT_TEMPLATES;

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
import io.stackgres.common.StackGresInitContainers;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContextUtil;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PostgresContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainers.RELOCATE_BINARIES)
public class InitRelocateBinaries implements ContainerFactory<DistributedLogsContainerContext> {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final VolumeMountsProvider<PostgresContainerContext> postgresExtensionsMounts;
  private final VolumeMountsProvider<ContainerContext> templateMounts;

  @Inject
  public InitRelocateBinaries(
      @ProviderName(CONTAINER_USER_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
      @ProviderName(POSTGRES_EXTENSIONS)
          VolumeMountsProvider<PostgresContainerContext> postgresExtensionsMounts,
      @ProviderName(SCRIPT_TEMPLATES)
          VolumeMountsProvider<ContainerContext> templateMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresExtensionsMounts = postgresExtensionsMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainers.RELOCATE_BINARIES.getName())
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
    return postgresExtensionsMounts.getDerivedEnvVars(ContextUtil.toPostgresContext(context));
  }

}
