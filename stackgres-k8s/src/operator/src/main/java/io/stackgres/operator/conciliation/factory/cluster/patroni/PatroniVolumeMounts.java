/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;

@ApplicationScoped
public class PatroniVolumeMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.PATRONI_ENV.getName())
            .withMountPath(ClusterStatefulSetPath.PATRONI_ENV_PATH
                .path(context.getClusterContext()))
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.PATRONI_CREDENTIALS.getName())
            .withMountPath(ClusterStatefulSetPath.PATRONI_SECRET_ENV_PATH
                .path(context.getClusterContext()))
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.PATRONI_CONFIG.getName())
            .withMountPath(ClusterStatefulSetPath.PATRONI_CONFIG_PATH.path())
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    return getDerivedEnvVars(context.getClusterContext());
  }

  private List<EnvVar> getDerivedEnvVars(ClusterContext context) {
    return List.of(
        ClusterStatefulSetEnvVars.PATRONI_ENV.envVar(context),
        ClusterStatefulSetPath.BASE_SECRET_PATH.envVar(context),
        ClusterStatefulSetPath.PATRONI_ENV_PATH.envVar(context),
        ClusterStatefulSetPath.PATRONI_SECRET_ENV_PATH.envVar(context),
        ClusterStatefulSetPath.PATRONI_CONFIG_PATH.envVar(context),
        ClusterStatefulSetPath.PATRONI_CONFIG_FILE_PATH.envVar(context)
    );
  }
}
