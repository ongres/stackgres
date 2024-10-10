/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.StackGresVolume;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LocalBinMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.LOCAL_BIN.getName())
            .withMountPath(ClusterPathV1.LOCAL_BIN_PATH.path())
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        ClusterPathV1.LOCAL_BIN_PATH.envVar(),
        ClusterPathV1.BASE_ENV_PATH.envVar(),
        ClusterPathV1.BASE_SECRET_PATH.envVar()
    );
  }
}
