/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.LOCAL_BIN;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.SCRIPT_TEMPLATES;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ClusterInitContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V12)
@InitContainer(ClusterInitContainer.SCRIPTS_SET_UP)
public class ScriptsSetUp implements ContainerFactory<StackGresClusterContainerContext> {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final VolumeMountsProvider<ContainerContext> localBinMounts;
  private final VolumeMountsProvider<ContainerContext> templateMounts;

  @Inject
  public ScriptsSetUp(
      @ProviderName(CONTAINER_USER_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
      @ProviderName(LOCAL_BIN)
          VolumeMountsProvider<ContainerContext> localBinMounts,
      @ProviderName(SCRIPT_TEMPLATES)
          VolumeMountsProvider<ContainerContext> templateMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.localBinMounts = localBinMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresComponent.KUBECTL.get(context.getClusterContext().getCluster())
            .findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(getClusterEnvVars(context))
        .withVolumeMounts(templateMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(localBinMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .build();
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContainerContext context) {
    return ImmutableList.<EnvVar>builder()
        .addAll(localBinMounts.getDerivedEnvVars(context))
        .addAll(templateMounts.getDerivedEnvVars(context))
        .add(new EnvVarBuilder()
            .withName("BASE_ENV_PATH")
            .withValue("/etc/env")
            .build())
        .add(new EnvVarBuilder()
            .withName("POSTGRES_PORT")
            .withValue(Integer.toString(EnvoyUtil.PG_PORT))
            .build())
        .add(ClusterStatefulSetPath.BASE_SECRET_PATH.envVar())
        .build();
  }

}
