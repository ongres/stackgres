/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.LocalBinMounts;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.SETUP_SCRIPTS)
public class ScriptsSetUp implements ContainerFactory<ClusterContainerContext> {

  private final ContainerUserOverrideMounts containerUserOverrideMounts;
  private final LocalBinMounts localBinMounts;
  private final ScriptTemplatesVolumeMounts templateMounts;

  @Inject
  KubectlUtil kubectl;

  @Inject
  public ScriptsSetUp(
      ContainerUserOverrideMounts containerUserOverrideMounts,
      LocalBinMounts localBinMounts,
      ScriptTemplatesVolumeMounts templateMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.localBinMounts = localBinMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainer.SETUP_SCRIPTS.getName())
        .withImage(kubectl.getImageName(context.getClusterContext().getCluster()))
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(getClusterEnvVars(context))
        .addToEnv(new EnvVarBuilder().withName("HOME").withValue("/tmp").build())
        .withVolumeMounts(templateMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(localBinMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .build();
  }

  private List<EnvVar> getClusterEnvVars(ClusterContainerContext context) {
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
