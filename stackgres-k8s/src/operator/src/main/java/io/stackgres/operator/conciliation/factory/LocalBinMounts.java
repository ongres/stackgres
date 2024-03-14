/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocalBinMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
        .withName(StackGresVolume.LOCAL_BIN.getName())
        .withMountPath(ClusterPath.LOCAL_BIN_PATH.path())
        .build(),
        new VolumeMountBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withMountPath(ClusterPath.LOCAL_BIN_START_PATRONI_SH_PATH.path())
        .withSubPath(ClusterPath.LOCAL_BIN_START_PATRONI_SH_PATH.filename())
        .build(),
        new VolumeMountBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withMountPath(ClusterPath.LOCAL_BIN_POST_INIT_SH_PATH.path())
        .withSubPath(ClusterPath.LOCAL_BIN_POST_INIT_SH_PATH.filename())
        .build(),
        new VolumeMountBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withMountPath(ClusterPath.LOCAL_BIN_EXEC_WITH_ENV_PATH.path())
        .withSubPath(ClusterPath.LOCAL_BIN_EXEC_WITH_ENV_PATH.filename())
        .build(),
        new VolumeMountBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withMountPath(ClusterPath.LOCAL_BIN_PATRONICTL_PATH.path())
        .withSubPath(ClusterPath.LOCAL_BIN_PATRONICTL_PATH.filename())
        .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        ClusterPath.LOCAL_BIN_PATH.envVar(),
        ClusterPath.BASE_ENV_PATH.envVar(),
        ClusterPath.BASE_SECRET_PATH.envVar()
    );
  }
}
