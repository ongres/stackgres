/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.VolumeConfig;
import io.stackgres.operator.common.VolumeConfig.VolumePathConfig;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupSecret;
import io.stackgres.operator.conciliation.factory.cluster.restore.RestoreConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.restore.RestoreSecret;
import org.jooq.lambda.Seq;

public enum ClusterStatefulSetVolumeConfig {

  DATA(VolumeConfig.persistentVolumeClaim(
      "data", ClusterStatefulSetPath.PG_BASE_PATH,
      ClusterStatefulSet::dataName)),
  SOCKET(VolumeConfig.inMemoryEmptyDir(
      "socket", ClusterStatefulSetPath.PG_RUN_PATH)),
  SHARED_MEMORY(VolumeConfig.inMemoryEmptyDir(
      "dshm", ClusterStatefulSetPath.SHARED_MEMORY_PATH)),
  SHARED(VolumeConfig.onDiskEmptyDir(
      "shared", ClusterStatefulSetPath.SHARED_PATH)),
  EMPTY_BASE(VolumeConfig.onDiskEmptyDir(
      "empty-base", ClusterStatefulSetPath.PG_BASE_PATH)),
  USER(VolumeConfig.onDiskEmptyDir(
      "user", ImmutableList.of(
          VolumePathConfig.of(ClusterStatefulSetPath.ETC_PASSWD_PATH,
              context -> true,
              volumeMountBuilder -> volumeMountBuilder.withReadOnly(true)),
          VolumePathConfig.of(ClusterStatefulSetPath.ETC_GROUP_PATH,
              context -> true,
              volumeMountBuilder -> volumeMountBuilder.withReadOnly(true)),
          VolumePathConfig.of(ClusterStatefulSetPath.ETC_SHADOW_PATH,
              context -> true,
              volumeMountBuilder -> volumeMountBuilder.withReadOnly(true)),
          VolumePathConfig.of(ClusterStatefulSetPath.ETC_GSHADOW_PATH,
              context -> true,
              volumeMountBuilder -> volumeMountBuilder.withReadOnly(true))))),
  LOCAL_BIN(VolumeConfig.onDiskEmptyDir(
      "local-bin", ClusterStatefulSetPath.LOCAL_BIN_PATH)),
  LOG(VolumeConfig.onDiskEmptyDir(
      "log", ClusterStatefulSetPath.PG_LOG_PATH)),
  PATRONI_ENV(VolumeConfig.configMap(
      "patroni-env", ClusterStatefulSetPath.PATRONI_ENV_PATH,
      PatroniConfigMap::name)),
  PATRONI_CONFIG(VolumeConfig.onDiskEmptyDir(
      "patroni-config", ClusterStatefulSetPath.PATRONI_CONFIG_PATH)),
  BACKUP_ENV(VolumeConfig.configMap(
      "backup-env", ClusterStatefulSetPath.BACKUP_ENV_PATH,
      BackupConfigMap::name)),
  BACKUP_SECRET(VolumeConfig.secret(
      "backup-secret", ClusterStatefulSetPath.BACKUP_SECRET_PATH,
      BackupSecret::name)),
  RESTORE_ENV(VolumeConfig.configMap(
      "restore-env", ClusterStatefulSetPath.RESTORE_ENV_PATH,
      RestoreConfigMap::name,
      context -> Optional.ofNullable(context.getCluster().getSpec())
          .map(StackGresClusterSpec::getInitData)
          .map(StackGresClusterInitData::getRestore)
          .map(StackGresClusterRestore::getFromBackup).isPresent())),
  RESTORE_SECRET(VolumeConfig.secret(
      "restore-secret", ClusterStatefulSetPath.RESTORE_SECRET_PATH,
      RestoreSecret::name,
      context -> Optional.ofNullable(context.getCluster().getSpec())
          .map(StackGresClusterSpec::getInitData)
          .map(StackGresClusterInitData::getRestore)
          .map(StackGresClusterRestore::getFromBackup).isPresent())),
  REPLICATE_ENV(VolumeConfig.configMap(
      "replicate-env", ClusterStatefulSetPath.REPLICATE_ENV_PATH,
      BackupConfigMap::name)),
  REPLICATE_SECRET(VolumeConfig.secret(
      "replicate-secret", ClusterStatefulSetPath.REPLICATE_SECRET_PATH,
      BackupSecret::name)),
  TEMPLATES(VolumeConfig.configMap(
      "templates", ClusterStatefulSetPath.TEMPLATES_PATH,
      TemplatesConfigMap::name));

  private final VolumeConfig volumeConfig;

  ClusterStatefulSetVolumeConfig(VolumeConfig volumeConfig) {
    this.volumeConfig = volumeConfig;
  }

  public static Stream<Volume> volumes(ClusterContext context) {
    return Seq.of(values())
        .map(ClusterStatefulSetVolumeConfig::config)
        .map(volumeConfig -> volumeConfig.volume(context))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public static Stream<Volume> allVolumes(ClusterContext context) {
    return Seq.of(values())
        .map(ClusterStatefulSetVolumeConfig::config)
        .map(volumeConfig -> volumeConfig.volume(context))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public VolumeConfig config() {
    return volumeConfig;
  }

  public static Stream<VolumeMount> volumeMounts(ClusterContext context,
      ClusterStatefulSetVolumeConfig... configs) {
    return Seq.of(configs)
        .map(ClusterStatefulSetVolumeConfig::config)
        .flatMap(volumeConfig -> volumeConfig.volumeMounts(context).stream());
  }

  public Collection<VolumeMount> volumeMounts(ClusterContext context) {
    return new ArrayList<>(volumeConfig.volumeMounts(context));
  }

  public VolumeMount volumeMount(ClusterContext context) {
    return volumeConfig.volumeMounts(context)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Volume mount " + volumeConfig.name() + " is not available for this context"));
  }

  public VolumeMount volumeMount(ClusterContext context,
      Function<VolumeMountBuilder,
      VolumeMountBuilder> volumeMountOverride) {
    return volumeConfig.volumeMounts(context)
        .stream()
        .map(VolumeMountBuilder::new)
        .map(volumeMountOverride)
        .map(VolumeMountBuilder::build)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Volume mount " + volumeConfig.name() + " is not available for this context"));
  }

  public VolumeMount volumeMount(ClusterStatefulSetPath path, ClusterContext context) {
    return volumeConfig.volumeMount(path, context)
        .orElseThrow(() -> new IllegalStateException(
            "Volume mount " + volumeConfig.name() + " with path " + path.path()
                + " and subPath " + path.subPath() + " is not available for this context"));
  }

  public Volume volume(ClusterContext context) {
    return volumeConfig.volume(context)
        .orElseThrow(() -> new IllegalStateException(
            "Volume " + volumeConfig.name()
                + " is not configured or not available for this context"));
  }

}
