/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.function.BiFunction;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.patroni.PatroniConfigMap;

public enum ClusterStatefulSetVolumeConfig {

  SOCKET("socket", ClusterStatefulSetPath.PG_RUN_PATH,
      ClusterStatefulSetVolumeConfig::createEmptyDirVolume),
  PATRONI_CONFIG("patroni-config", ClusterStatefulSetPath.PATRONI_ENV_PATH,
      ClusterStatefulSetVolumeConfig::createConfigMapVolume,
      PatroniConfigMap::name),
  BACKUP_CONFIG("backup-config", ClusterStatefulSetPath.BACKUP_ENV_PATH,
      ClusterStatefulSetVolumeConfig::createConfigMapVolume,
      BackupConfigMap::name),
  BACKUP_SECRET("backup-secret", ClusterStatefulSetPath.BACKUP_SECRET_PATH,
      ClusterStatefulSetVolumeConfig::createSecretVolume,
      BackupSecret::name),
  RESTORE_CONFIG("restore-config", ClusterStatefulSetPath.RESTORE_ENV_PATH,
      ClusterStatefulSetVolumeConfig::createConfigMapVolume,
      RestoreConfigMap::name),
  RESTORE_SECRET("restore-secret", ClusterStatefulSetPath.RESTORE_SECRET_PATH,
      ClusterStatefulSetVolumeConfig::createSecretVolume,
      RestoreSecret::name),
  RESTORE_ENTRYPOINT("restore-entrypoint", ClusterStatefulSetPath.RESTORE_ENTRYPOINT_PATH,
      ClusterStatefulSetVolumeConfig::createEmptyDirVolume),
  LOCAL_BIN("local-bin", ClusterStatefulSetPath.LOCAL_BIN_PATH,
      ClusterStatefulSetVolumeConfig::createEmptyDirVolume);

  private final String name;
  private final ClusterStatefulSetPath path;
  private final VolumeMount volumeMount;
  private final Function<StackGresClusterContext, Volume> volumeFactory;
  private final Function<StackGresClusterContext, String> getResourceName;

  private ClusterStatefulSetVolumeConfig(String name, ClusterStatefulSetPath path,
      BiFunction<StackGresClusterContext, ClusterStatefulSetVolumeConfig, Volume> volumeFactory) {
    this(name, path, volumeFactory, context -> {
      throw new UnsupportedOperationException();
    });
  }

  private ClusterStatefulSetVolumeConfig(String name, ClusterStatefulSetPath path,
      BiFunction<StackGresClusterContext, ClusterStatefulSetVolumeConfig, Volume> volumeFactory,
      Function<StackGresClusterContext, String> getResourceName) {
    this.name = name;
    this.path = path;
    this.volumeMount = new VolumeMountBuilder()
        .withName(name)
        .withMountPath(path.path())
        .build();
    this.volumeFactory = context -> volumeFactory.apply(context, this);
    this.getResourceName = getResourceName;
  }

  public String volumeName() {
    return name;
  }

  public String path() {
    return path.path();
  }

  public VolumeMount volumeMount() {
    return volumeMount;
  }

  public Function<StackGresClusterContext, Volume> volumeFactory() {
    return volumeFactory;
  }

  private static Volume createEmptyDirVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return new VolumeBuilder()
        .withName(config.volumeName())
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build();
  }

  private static Volume createConfigMapVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return new VolumeBuilder()
        .withName(config.volumeName())
        .withNewConfigMap()
        .withName(config.getResourceName.apply(context))
        .withDefaultMode(444)
        .endConfigMap()
        .build();
  }

  private static Volume createSecretVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return new VolumeBuilder()
        .withName(config.volumeName())
        .withNewSecret()
        .withSecretName(config.getResourceName.apply(context))
        .withDefaultMode(444)
        .endSecret()
        .build();
  }
}
