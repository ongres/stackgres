/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.patroni.PatroniConfigMap;

public enum ClusterStatefulSetVolumeConfig {

  DATA("data", ClusterStatefulSetPath.PG_DATA_PATH,
      ClusterStatefulSet::dataName),
  BACKUPS("backups", ClusterStatefulSetPath.BACKUPS_PATH,
      ClusterStatefulSet::backupName),
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
  private final Function<StackGresClusterContext, VolumeMount> volumeMountFactory;
  private final Function<StackGresClusterContext, Optional<Volume>> volumeFactory;
  private final Function<StackGresClusterContext, String> getResourceName;

  private ClusterStatefulSetVolumeConfig(String name, ClusterStatefulSetPath path,
      BiFunction<StackGresClusterContext, ClusterStatefulSetVolumeConfig,
          Optional<Volume>> volumeFactory) {
    this(name, path, volumeFactory, context -> name, context -> {
      throw new UnsupportedOperationException();
    });
  }

  private ClusterStatefulSetVolumeConfig(String name, ClusterStatefulSetPath path,
      Function<StackGresClusterContext, String> getName) {
    this(name, path, ClusterStatefulSetVolumeConfig::noVolume, getName, context -> {
      throw new UnsupportedOperationException();
    });
  }

  private ClusterStatefulSetVolumeConfig(String name, ClusterStatefulSetPath path,
      BiFunction<StackGresClusterContext, ClusterStatefulSetVolumeConfig,
          Optional<Volume>> volumeFactory,
      Function<StackGresClusterContext, String> getResourceName) {
    this(name, path, volumeFactory, context -> name, getResourceName);
  }

  private ClusterStatefulSetVolumeConfig(String name, ClusterStatefulSetPath path,
      BiFunction<StackGresClusterContext, ClusterStatefulSetVolumeConfig,
          Optional<Volume>> volumeFactory,
      Function<StackGresClusterContext, String> getName,
      Function<StackGresClusterContext, String> getResourceName) {
    this.name = name;
    this.path = path;
    this.volumeMountFactory = context -> new VolumeMountBuilder()
        .withName(getName.apply(context))
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

  public Function<StackGresClusterContext, VolumeMount> volumeMountFactory() {
    return volumeMountFactory;
  }

  public Function<StackGresClusterContext, Optional<Volume>> volumeFactory() {
    return volumeFactory;
  }

  private static Optional<Volume> createEmptyDirVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.volumeName())
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build());
  }

  private static Optional<Volume> createConfigMapVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.volumeName())
        .withNewConfigMap()
        .withName(config.getResourceName.apply(context))
        .withDefaultMode(444)
        .endConfigMap()
        .build());
  }

  private static Optional<Volume> createSecretVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.volumeName())
        .withNewSecret()
        .withSecretName(config.getResourceName.apply(context))
        .withDefaultMode(444)
        .endSecret()
        .build());
  }

  private static Optional<Volume> noVolume(StackGresClusterContext context,
      ClusterStatefulSetVolumeConfig config) {
    return Optional.empty();
  }
}
