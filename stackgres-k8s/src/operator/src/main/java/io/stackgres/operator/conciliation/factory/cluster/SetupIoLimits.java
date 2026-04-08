/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.CgroupMounts;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.SETUP_IO_LIMITS)
public class SetupIoLimits implements ContainerFactory<ClusterContainerContext> {

  private final UserOverrideMounts userOverrideMounts;

  private final TemplatesMounts templateMounts;

  private final CgroupMounts cgroupMounts;

  @Inject
  public SetupIoLimits(
      UserOverrideMounts userOverrideMounts,
      TemplatesMounts templateMounts,
      CgroupMounts cgroupMounts) {
    this.userOverrideMounts = userOverrideMounts;
    this.templateMounts = templateMounts;
    this.cgroupMounts = cgroupMounts;
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return Optional.of(context.getClusterContext().getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getPersistentVolume)
        .map(StackGresClusterPodsPersistentVolume::getIoLimits)
        .map(ioLimits -> ioLimits.getReadIops() != null
            || ioLimits.getWriteIops() != null
            || ioLimits.getReadMiBps() != null
            || ioLimits.getWriteMiBps() != null)
        .orElse(false);
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final String patroniImageName = StackGresUtil.getPatroniImageName(clusterContext.getCluster());
    return new ContainerBuilder()
        .withName(StackGresInitContainer.SETUP_IO_LIMITS.getName())
        .withImage(patroniImageName)
        .withImagePullPolicy(getDefaultPullPolicy())
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_SETUP_IO_LIMITS_SH_PATH.filename())
        .withNewSecurityContext()
        .withRunAsUser(0L)
        .withNewCapabilities()
        .withDrop("ALL")
        .withAdd("CHOWN")
        .endCapabilities()
        .endSecurityContext()
        .addAllToEnv(templateMounts.getDerivedEnvVars(context))
        .addToEnv(
            new EnvVarBuilder()
            .withName("HOME")
            .withValue("/tmp")
            .build(),
            new EnvVarBuilder()
            .withName("POD_UID")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.uid"))
                .build())
            .build())
        .addAllToEnv(cgroupMounts.getDerivedEnvVars(context))
        .addAllToVolumeMounts(templateMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(userOverrideMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(cgroupMounts.getVolumeMounts(context))
        .build();
  }

}
