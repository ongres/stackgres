/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.factory.ClusterInitContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V12)
@InitContainer(ClusterInitContainer.USER_SET_UP)
public class UserSetUp implements ContainerFactory<StackGresClusterContainerContext> {

  private final VolumeMountsProvider<ContainerContext> scriptTemplateMounts;

  private final VolumeMountsProvider<ContainerContext> postgresSocketMounts;

  @Inject
  public UserSetUp(
      @ProviderName(VolumeMountProviderName.SCRIPT_TEMPLATES)
          VolumeMountsProvider<ContainerContext> scriptTemplateMounts,
      @ProviderName(VolumeMountProviderName.POSTGRES_DATA)
          VolumeMountsProvider<ContainerContext> postgresSocketMounts) {
    this.scriptTemplateMounts = scriptTemplateMounts;
    this.postgresSocketMounts = postgresSocketMounts;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresComponent.KUBECTL.get(context.getClusterContext().getCluster())
            .findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(getClusterEnvVars(context))
        .withVolumeMounts(scriptTemplateMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(PatroniStaticVolume.USER.getVolumeName())
                .withMountPath("/local/etc")
                .withSubPath("etc")
                .build())
        .build();
  }

  private List<EnvVar> getClusterEnvVars(StackGresClusterContainerContext context) {

    return ImmutableList.<EnvVar>builder()
        .addAll(scriptTemplateMounts.getDerivedEnvVars(context))
        .addAll(postgresSocketMounts.getDerivedEnvVars(context))
        .build();

  }
}
