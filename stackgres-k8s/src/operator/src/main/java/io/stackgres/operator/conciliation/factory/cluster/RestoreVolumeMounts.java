/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;

@ApplicationScoped
public class RestoreVolumeMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.RESTORE_ENV.getVolumeName())
            .withMountPath("/etc/env/restore")
            .build(),
        new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.RESTORE_CREDENTIALS.getVolumeName())
            .withMountPath("/etc/env/.secret/restore")
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return List.of(
        new EnvVarBuilder()
            .withName("RESTORE_ENV")
            .withValue("restore")
            .build(),
        new EnvVarBuilder()
            .withName("RESTORE_ENV_PATH")
            .withValue("/etc/env/restore")
            .build(),
        new EnvVarBuilder()
            .withName("RESTORE_SECRET_PATH")
            .withValue("/etc/env/.secret/restore")
            .build()
    );
  }
}
