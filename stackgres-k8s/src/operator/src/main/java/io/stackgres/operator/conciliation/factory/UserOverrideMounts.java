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
public class UserOverrideMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterPath.ETC_PASSWD_PATH.path())
            .withSubPath(ClusterPath.ETC_PASSWD_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterPath.ETC_GROUP_PATH.path())
            .withSubPath(ClusterPath.ETC_GROUP_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterPath.ETC_SHADOW_PATH.path())
            .withSubPath(ClusterPath.ETC_SHADOW_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterPath.ETC_GSHADOW_PATH.path())
            .withSubPath(ClusterPath.ETC_GSHADOW_PATH.subPath())
            .withReadOnly(true)
            .build());
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        ClusterPath.ETC_PASSWD_PATH.envVar(),
        ClusterPath.ETC_GROUP_PATH.envVar(),
        ClusterPath.ETC_SHADOW_PATH.envVar(),
        ClusterPath.ETC_GSHADOW_PATH.envVar()
    );
  }
}
