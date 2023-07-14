/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVolume;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ContainerUserOverrideMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterStatefulSetPath.ETC_PASSWD_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_PASSWD_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterStatefulSetPath.ETC_GROUP_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_GROUP_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterStatefulSetPath.ETC_SHADOW_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_SHADOW_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.USER.getName())
            .withMountPath(ClusterStatefulSetPath.ETC_GSHADOW_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_GSHADOW_PATH.subPath())
            .withReadOnly(true)
            .build());
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        ClusterStatefulSetPath.ETC_PASSWD_PATH.envVar(),
        ClusterStatefulSetPath.ETC_GROUP_PATH.envVar(),
        ClusterStatefulSetPath.ETC_SHADOW_PATH.envVar(),
        ClusterStatefulSetPath.ETC_GSHADOW_PATH.envVar()
    );
  }
}
