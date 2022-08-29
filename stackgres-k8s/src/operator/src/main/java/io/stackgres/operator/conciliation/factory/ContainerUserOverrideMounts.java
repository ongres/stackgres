/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;

@ApplicationScoped
public class ContainerUserOverrideMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(PatroniStaticVolume.USER.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.ETC_PASSWD_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_PASSWD_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(PatroniStaticVolume.USER.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.ETC_GROUP_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_GROUP_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(PatroniStaticVolume.USER.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.ETC_SHADOW_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_SHADOW_PATH.subPath())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(PatroniStaticVolume.USER.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.ETC_GSHADOW_PATH.path())
            .withSubPath(ClusterStatefulSetPath.ETC_GSHADOW_PATH.subPath())
            .withReadOnly(true)
            .build());
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        new EnvVarBuilder()
            .withName("ETC_PASSWD_PATH")
            .withValue(ClusterStatefulSetPath.ETC_PASSWD_PATH.path())
            .build(),
        new EnvVarBuilder()
            .withName("ETC_GROUP_PATH")
            .withValue(ClusterStatefulSetPath.ETC_GROUP_PATH.path())
            .build(),
        new EnvVarBuilder()
            .withName("ETC_SHADOW_PATH")
            .withValue(ClusterStatefulSetPath.ETC_SHADOW_PATH.path())
            .build(),
        new EnvVarBuilder()
            .withName("ETC_GSHADOW_PATH")
            .withValue(ClusterStatefulSetPath.ETC_GSHADOW_PATH.path())
            .build()
    );
  }
}
