/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterEnvVar;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PatroniMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
        .withName(StackGresVolume.PATRONI_ENV.getName())
        .withMountPath(ClusterPath.PATRONI_ENV_PATH
            .path(context.getClusterContext()))
        .build(),
        new VolumeMountBuilder()
        .withName(StackGresVolume.PATRONI_CREDENTIALS.getName())
        .withMountPath(ClusterPath.PATRONI_SECRET_ENV_PATH
            .path(context.getClusterContext()))
        .build(),
        new VolumeMountBuilder()
        .withName(StackGresVolume.PATRONI_CONFIG.getName())
        .withMountPath(ClusterPath.PATRONI_CONFIG_PATH.path())
        .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    return getDerivedEnvVars(context.getClusterContext());
  }

  private List<EnvVar> getDerivedEnvVars(ClusterContext context) {
    return List.of(
        ClusterEnvVar.PATRONI_ENV.envVar(context),
        ClusterPath.BASE_SECRET_PATH.envVar(context),
        ClusterPath.PATRONI_ENV_PATH.envVar(context),
        ClusterPath.PATRONI_SECRET_ENV_PATH.envVar(context),
        ClusterPath.PATRONI_CONFIG_PATH.envVar(context),
        ClusterPath.PATRONI_CONFIG_FILE_PATH.envVar(context)
    );
  }
}
