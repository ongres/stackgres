/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupScriptTemplatesVolumeMounts
    implements VolumeMountsProvider<StackGresClusterContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresClusterContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withSubPath(ClusterPathV1.LOCAL_BIN_CREATE_BACKUP_SH_PATH.filename())
            .withMountPath(ClusterPathV1.LOCAL_BIN_CREATE_BACKUP_SH_PATH.path())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withSubPath(ClusterPathV1.LOCAL_BIN_SHELL_UTILS_PATH.filename())
            .withMountPath(ClusterPathV1.LOCAL_BIN_SHELL_UTILS_PATH.path())
            .withReadOnly(true)
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(StackGresClusterContext context) {
    return List.of(
        ClusterPathV1.TEMPLATES_PATH.envVar()
    );
  }
}
