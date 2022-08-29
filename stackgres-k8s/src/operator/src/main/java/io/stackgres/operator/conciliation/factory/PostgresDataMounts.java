/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;

@ApplicationScoped
public class PostgresDataMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {

    return ImmutableList.<VolumeMount>builder()
        .add(new VolumeMountBuilder()
            .withName(context.getDataVolumeName())
            .withMountPath(ClusterStatefulSetPath.PG_BASE_PATH.path())
            .build())
        .build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return ImmutableList.<EnvVar>builder()
        .add(new EnvVarBuilder()
            .withName("PG_BASE_PATH")
            .withValue(ClusterStatefulSetPath.PG_BASE_PATH.path())
            .build())
        .add(new EnvVarBuilder()
            .withName("PG_DATA_PATH")
            .withValue(ClusterStatefulSetPath.PG_DATA_PATH.path())
            .build())
        .build();
  }
}
