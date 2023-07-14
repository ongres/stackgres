/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupScriptTemplatesVolumeMounts
    implements VolumeMountsProvider<StackGresBackupContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresBackupContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH.filename())
            .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_CREATE_BACKUP_SH_PATH.path())
            .withReadOnly(true)
            .build(),
        new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_SHELL_UTILS_PATH.filename())
            .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_SHELL_UTILS_PATH.path())
            .withReadOnly(true)
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(StackGresBackupContext context) {
    return List.of(
        ClusterStatefulSetPath.TEMPLATES_PATH.envVar()
    );
  }
}
