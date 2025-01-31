/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterEnvVar;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReplicateMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.REPLICATE_ENV.getName())
            .withMountPath(ClusterPath.REPLICATE_ENV_PATH.path(clusterContext))
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.REPLICATE_CREDENTIALS.getName())
            .withMountPath(ClusterPath.REPLICATE_SECRET_PATH.path(clusterContext))
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();
    return List.of(
        ClusterEnvVar.REPLICATE_ENV.envVar(clusterContext),
        ClusterPath.REPLICATE_ENV_PATH.envVar(clusterContext),
        ClusterPath.REPLICATE_SECRET_PATH.envVar(clusterContext)
    );
  }
}
