/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.SETUP_DATA_PATHS)
public class DataPathsInitializer implements ContainerFactory<ClusterContainerContext> {

  private final PostgresDataMounts postgresDataMounts;

  private final ScriptTemplatesVolumeMounts scriptTemplateMounts;

  private final ContainerUserOverrideMounts containerUserOverride;

  @Inject
  KubectlUtil kubectl;

  @Inject
  public DataPathsInitializer(
      PostgresDataMounts postgresDataMounts,
      ScriptTemplatesVolumeMounts scriptTemplateMounts,
      ContainerUserOverrideMounts containerUserOverride) {
    this.postgresDataMounts = postgresDataMounts;
    this.scriptTemplateMounts = scriptTemplateMounts;
    this.containerUserOverride = containerUserOverride;
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainer.SETUP_DATA_PATHS.getName())
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

  private List<EnvVar> getClusterEnvVars(ClusterContainerContext context) {
    return postgresDataMounts.getDerivedEnvVars(context);
  }

}
