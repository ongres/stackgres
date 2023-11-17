/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostgresDataMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
        .add(new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        ClusterPath.PG_BASE_PATH.envVar(),
        ClusterPath.PG_DATA_PATH.envVar()
        );
  }
}
