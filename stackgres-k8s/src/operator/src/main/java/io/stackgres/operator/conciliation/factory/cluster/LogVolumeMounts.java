/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogVolumeMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.LOG.getName())
            .withMountPath(ClusterStatefulSetPath.PG_LOG_PATH.path())
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        ClusterStatefulSetPath.PG_LOG_PATH.envVar()
    );
  }
}
