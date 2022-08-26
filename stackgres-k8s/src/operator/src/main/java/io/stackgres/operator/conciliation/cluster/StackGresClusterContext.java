/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

@Value.Immutable
public interface StackGresClusterContext extends GenerationContext<StackGresCluster>,
    ClusterContext {

  @Override
  @Value.Derived
  default StackGresCluster getCluster() {
    return getSource();
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  Optional<StackGresBackupConfig> getBackupConfig();

  Optional<StackGresObjectStorage> getObjectStorageConfig();

  StackGresPostgresConfig getPostgresConfig();

  StackGresProfile getProfile();

  Optional<StackGresPoolingConfig> getPoolingConfig();

  Optional<StackGresBackup> getRestoreBackup();

  Optional<Prometheus> getPrometheus();

  Optional<Secret> getDatabaseSecret();

  Set<String> getClusterBackupNamespaces();

  Optional<Secret> getExternalSuperuserUsernameSecret();

  Optional<Secret> getExternalSuperuserPasswordSecret();

  Optional<Secret> getExternalReplicationUsernameSecret();

  Optional<Secret> getExternalReplicationPasswordSecret();

  Optional<Secret> getExternalAuthenticatorUsernameSecret();

  Optional<Secret> getExternalAuthenticatorPasswordSecret();

  default Optional<String> getBackupPath() {
    Optional<@NotNull StackGresClusterConfiguration> config = Optional.of(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfiguration);

    return config
        .map(StackGresClusterConfiguration::getBackupPath)
        .or(() -> config
            .map(StackGresClusterConfiguration::getBackups)
            .map(Collection::stream)
            .flatMap(Stream::findFirst)
            .map(StackGresClusterBackupConfiguration::getPath));
  }

  default Optional<BackupConfiguration> getBackupConfiguration() {
    if (getObjectStorageConfig().isPresent()) {
      return Optional.of(getCluster())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getConfiguration)
          .map(StackGresClusterConfiguration::getBackups)
          .map(Collection::stream)
          .flatMap(Stream::findFirst)
          .map(bc -> new BackupConfiguration(
              bc.getRetention(),
              bc.getCronSchedule(),
              bc.getCompression(),
              bc.getPath(),
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwidth(),
                      bp.getMaxDiskBandwidth(),
                      bp.getUploadDiskConcurrency()))
                  .orElse(null)));
    } else {
      return getBackupConfig()
          .map(StackGresBackupConfig::getSpec)
          .map(StackGresBackupConfigSpec::getBaseBackups)
          .map(bc -> new BackupConfiguration(
              bc.getRetention(),
              bc.getCronSchedule(),
              bc.getCompression(),
              Optional.of(getCluster())
                  .map(StackGresCluster::getSpec)
                  .map(StackGresClusterSpec::getConfiguration)
                  .map(StackGresClusterConfiguration::getBackupPath)
                  .orElse(null),
              Optional.ofNullable(bc.getPerformance())
                  .map(bp -> new BackupPerformance(
                      bp.getMaxNetworkBandwidth(),
                      bp.getMaxDiskBandwidth(),
                      bp.getUploadDiskConcurrency()))
                  .orElse(null)));
    }
  }

  default Optional<BackupStorage> getBackupStorage() {
    return getObjectStorageConfig().map(CustomResource::getSpec)
        .or(() -> getBackupConfig().map(StackGresBackupConfig::getSpec)
            .map(StackGresBackupConfigSpec::getStorage));
  }

  default String getConfigCrdName() {
    if (getObjectStorageConfig().isPresent()) {
      return HasMetadata.getFullResourceName(StackGresObjectStorage.class);
    } else {
      return HasMetadata.getFullResourceName(StackGresBackupConfig.class);
    }
  }

  default Optional<ObjectMeta> getBackupConfigurationMetadata() {
    return getObjectStorageConfig()
        .map(HasMetadata::getMetadata)
        .or(() -> getBackupConfig().map(CustomResource::getMetadata));
  }

  default Optional<String> getBackupConfigurationCustomResourceName() {
    return getBackupConfigurationMetadata()
        .map(ObjectMeta::getName);
  }

  default Optional<String> getBackupConfigurationResourceVersion() {
    return getBackupConfigurationMetadata()
        .map(ObjectMeta::getResourceVersion);
  }

}
