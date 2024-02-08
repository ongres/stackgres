/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static io.stackgres.operator.common.CryptoUtil.generatePassword;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicateFromStorage;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

@Value.Immutable
public interface StackGresClusterContext extends GenerationContext<StackGresCluster>,
    ClusterContext {

  StackGresConfig getConfig();

  Optional<VersionInfo> getKubernetesVersion();

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

  Optional<StackGresObjectStorage> getObjectStorage();

  Optional<StackGresCluster> getReplicateCluster();

  Optional<StackGresObjectStorage> getReplicationInitializationObjectStorageConfig();

  Optional<StackGresObjectStorage> getReplicateObjectStorageConfig();

  StackGresPostgresConfig getPostgresConfig();

  StackGresProfile getProfile();

  Optional<StackGresPoolingConfig> getPoolingConfig();

  Map<String, Secret> getBackupSecrets();

  Optional<StackGresBackup> getRestoreBackup();

  Map<String, Secret> getRestoreSecrets();

  Map<String, Secret> getReplicationInitializationSecrets();

  Map<String, Secret> getReplicateSecrets();

  Optional<PrometheusContext> getPrometheusContext();

  Optional<Secret> getDatabaseSecret();

  Set<String> getClusterBackupNamespaces();

  Optional<String> getSuperuserUsername();

  Optional<String> getSuperuserPassword();

  @Value.Derived
  default String getGeneratedSuperuserPassword() {
    return generatePassword();
  }

  Optional<String> getReplicationUsername();

  Optional<String> getReplicationPassword();

  @Value.Derived
  default String getGeneratedReplicationPassword() {
    return generatePassword();
  }

  Optional<String> getAuthenticatorUsername();

  Optional<String> getAuthenticatorPassword();

  Optional<String> getUserPasswordForBinding();

  @Value.Derived
  default String getGeneratedAuthenticatorPassword() {
    return generatePassword();
  }

  Optional<String> getPatroniRestApiPassword();

  @Value.Derived
  default String getGeneratedPatroniRestApiPassword() {
    return generatePassword();
  }

  @Value.Derived
  default String getGeneratedBabelfishPassword() {
    return generatePassword();
  }

  @Value.Derived
  default String getGeneratedPgBouncerAdminPassword() {
    return generatePassword();
  }

  @Value.Derived
  default String getGeneratedPgBouncerStatsPassword() {
    return generatePassword();
  }

  Optional<String> getPostgresSslCertificate();

  Optional<String> getPostgresSslPrivateKey();

  Optional<StackGresBackup> getReplicationInitializationBackup();

  Optional<StackGresBackup> getReplicationInitializationBackupToCreate();

  default Optional<String> getBackupPath() {
    Optional<@NotNull StackGresClusterConfigurations> config = Optional.of(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations);

    return config
        .map(StackGresClusterConfigurations::getBackups)
        .map(Collection::stream)
        .flatMap(Stream::findFirst)
        .map(StackGresClusterBackupConfiguration::getPath);
  }

  default Optional<BackupConfiguration> getBackupConfiguration() {
    return Optional.of(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
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
                    bp.getUploadDiskConcurrency(),
                    bp.getUploadConcurrency(),
                    bp.getDownloadConcurrency()))
                .orElse(null),
            Optional.ofNullable(bc.getUseVolumeSnapshot())
            .orElse(false),
            bc.getVolumeSnapshotClass(),
            bc.getFastVolumeSnapshot(),
            bc.getTimeout(),
            bc.getReconciliationTimeout()));
  }

  default Optional<BackupStorage> getBackupStorage() {
    return getObjectStorage().map(CustomResource::getSpec);
  }

  default String getConfigCrdName() {
    return HasMetadata.getFullResourceName(StackGresObjectStorage.class);
  }

  default Optional<ObjectMeta> getBackupConfigurationMetadata() {
    return getObjectStorage()
        .map(HasMetadata::getMetadata);
  }

  default Optional<String> getBackupConfigurationCustomResourceName() {
    return getBackupConfigurationMetadata()
        .map(ObjectMeta::getName);
  }

  default Optional<String> getBackupConfigurationResourceVersion() {
    return getBackupConfigurationMetadata()
        .map(ObjectMeta::getResourceVersion);
  }

  default Optional<String> getReplicationInitializationPath() {
    return getReplicationInitializationBackup()
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupPath);
  }

  default Optional<String> getReplicatePath() {
    return getReplicateCluster()
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
        .stream()
        .flatMap(List::stream)
        .findFirst()
        .map(StackGresClusterBackupConfiguration::getPath)
        .or(() -> Optional.of(getCluster())
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getReplicateFrom)
            .map(StackGresClusterReplicateFrom::getStorage)
            .map(StackGresClusterReplicateFromStorage::getPath));
  }

  default Optional<BackupStorage> getReplicationInitializationStorage() {
    return getReplicationInitializationObjectStorageConfig()
        .map(CustomResource::getSpec);
  }

  default Optional<BackupStorage> getReplicateStorage() {
    return getReplicateObjectStorageConfig()
        .map(CustomResource::getSpec);
  }

  default Optional<BackupConfiguration> getReplicationInitializationConfiguration() {
    return getReplicationInitializationBackup()
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupPath)
        .map(path -> new BackupConfiguration(
            null,
            null,
            null,
            path,
            Optional.ofNullable(getCluster().getSpec().getReplication()
                .getInitialization().getBackupRestorePerformance())
            .map(bp -> new BackupPerformance(
                bp.getMaxNetworkBandwidth(),
                bp.getMaxDiskBandwidth(),
                bp.getUploadDiskConcurrency(),
                bp.getUploadConcurrency(),
                bp.getDownloadConcurrency()))
            .orElse(null),
            null,
            null,
            null,
            null,
            null));
  }

  default Optional<BackupConfiguration> getReplicateConfiguration() {
    return Optional.of(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getReplicateFrom)
        .map(StackGresClusterReplicateFrom::getStorage)
        .map(bc -> new BackupConfiguration(
            null,
            null,
            null,
            bc.getPath(),
            Optional.ofNullable(bc.getPerformance())
            .map(bp -> new BackupPerformance(
                bp.getMaxNetworkBandwidth(),
                bp.getMaxDiskBandwidth(),
                bp.getUploadDiskConcurrency(),
                bp.getUploadConcurrency(),
                bp.getDownloadConcurrency()))
            .orElse(null),
            null,
            null,
            null,
            null,
            null));
  }

  default Map<String, String> clusterPodsCustomLabels() {
    return Optional.ofNullable(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getLabels)
        .map(StackGresClusterSpecLabels::getClusterPods)
        .orElse(Map.of());
  }

  default Map<String, String> servicesCustomLabels() {
    return Optional.ofNullable(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getLabels)
        .map(StackGresClusterSpecLabels::getServices)
        .orElse(Map.of());
  }

}
