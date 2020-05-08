/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.VolumeConfig;
import io.stackgres.operator.patroni.factory.PatroniConfigMap;
import org.jooq.lambda.Seq;

public enum ClusterStatefulSetVolumeConfig {

  DATA(VolumeConfig.persistentVolumeClaim(
      "data", ClusterStatefulSetPath.PG_BASE_PATH,
      ClusterStatefulSet::dataName)),
  SOCKET(VolumeConfig.emptyDir(
      "socket", ClusterStatefulSetPath.PG_RUN_PATH)),
  LOCAL_BIN(VolumeConfig.emptyDir(
      "local-bin", ClusterStatefulSetPath.LOCAL_BIN_PATH)),
  PATRONI_CONFIG(VolumeConfig.configMap(
      "patroni-config", ClusterStatefulSetPath.PATRONI_ENV_PATH,
      PatroniConfigMap::name)),
  BACKUP_CONFIG(VolumeConfig.configMap(
      "backup-config", ClusterStatefulSetPath.BACKUP_ENV_PATH,
      BackupConfigMap::name)),
  BACKUP_SECRET(VolumeConfig.secret(
      "backup-secret", ClusterStatefulSetPath.BACKUP_SECRET_PATH,
      BackupSecret::name)),
  RESTORE_CONFIG(VolumeConfig.configMap(
      "restore-config", ClusterStatefulSetPath.RESTORE_ENV_PATH,
      RestoreConfigMap::name,
      context ->  context.getRestoreContext().isPresent())),
  RESTORE_SECRET(VolumeConfig.secret(
      "restore-secret", ClusterStatefulSetPath.RESTORE_SECRET_PATH,
      RestoreSecret::name,
      context ->  context.getRestoreContext().isPresent())),
  RESTORE_ENTRYPOINT(VolumeConfig.emptyDir(
      "restore-entrypoint", ClusterStatefulSetPath.RESTORE_ENTRYPOINT_PATH,
      context ->  context.getRestoreContext().isPresent()));

  private final VolumeConfig volumeConfig;

  ClusterStatefulSetVolumeConfig(VolumeConfig volumeConfig) {
    this.volumeConfig = volumeConfig;
  }

  public VolumeConfig config() {
    return volumeConfig;
  }

  public VolumeMount volumeMount(StackGresClusterContext context) {
    return volumeConfig.volumeMount(context)
        .orElseThrow(() -> new IllegalStateException(
            "Volume mount " + volumeConfig.name() + " is not available for this context"));
  }

  public VolumeMount volumeMount(StackGresClusterContext context, String mountPath) {
    return new VolumeMountBuilder(volumeMount(context))
        .withMountPath(mountPath)
        .build();
  }

  public VolumeMount volumeMount(StackGresClusterContext context,
      String mountPath, String subPath) {
    return new VolumeMountBuilder(volumeMount(context))
        .withMountPath(mountPath)
        .withSubPath(subPath)
        .build();
  }

  public Volume volume(StackGresClusterContext context) {
    return volumeConfig.volume(context)
        .orElseThrow(() -> new IllegalStateException(
            "Volume " + volumeConfig.name()
            + " is not configured or not available for this context"));
  }

  public static Stream<VolumeMount> volumeMounts(StackGresClusterContext context) {
    return Seq.of(values())
        .map(ClusterStatefulSetVolumeConfig::config)
        .map(volumeConfig -> volumeConfig.volumeMount(context))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public static Stream<Volume> volumes(StackGresClusterContext context) {
    return Seq.of(values())
        .map(ClusterStatefulSetVolumeConfig::config)
        .map(volumeConfig -> volumeConfig.volume(context))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

}
