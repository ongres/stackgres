/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.restore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackupVolumeSnapshotStatus;
import io.stackgres.common.crd.sgbackup.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.backup.AbstractBackupConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StackGresVolume.RESTORE_ENV.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.RESTORE_ENV.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .build())
        .build();
  }

  private ConfigMap buildSource(StackGresClusterContext context) {
    final Optional<StackGresBackup> restoreBackup = context.getRestoreBackup();
    final Map<String, String> data = new HashMap<>();
    final StackGresCluster cluster = context.getSource();

    if (restoreBackup.isPresent()) {
      final String status = restoreBackup
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getProcess)
          .map(StackGresBackupProcess::getStatus)
          .orElse(BackupStatus.PENDING.status());

      if (!BackupStatus.COMPLETED.status().equals(status)) {
        data.put("RESTORE_BACKUP_ERROR", "Backup is " + status);
      } else {
        final StackGresBackup backup = restoreBackup.get();
        data.put("BACKUP_RESOURCE_VERSION",
            backup.getMetadata().getResourceVersion());
        data.put("RESTORE_BACKUP_ID",
            backup.getStatus().getInternalName());
        data.put("RESTORE_VOLUME_SNAPSHOT",
            Optional.of(backup.getStatus())
            .map(StackGresBackupStatus::getVolumeSnapshot)
            .map(ignored -> Boolean.TRUE)
            .map(String::valueOf)
            .orElse(Boolean.FALSE.toString()));
        data.put("RESTORE_BACKUP_LABEL",
            Optional.of(backup.getStatus())
            .map(StackGresBackupStatus::getVolumeSnapshot)
            .map(StackGresBackupVolumeSnapshotStatus::getBackupLabel)
            .orElse(""));
        data.put("RESTORE_TABLESPACE_MAP",
            Optional.of(backup.getStatus())
            .map(StackGresBackupStatus::getVolumeSnapshot)
            .map(StackGresBackupVolumeSnapshotStatus::getTablespaceMap)
            .orElse(""));

        data.putAll(getBackupEnvVars(context,
            Optional.of(backup)
                .map(StackGresBackup::getStatus)
                .map(StackGresBackupStatus::getBackupPath)
                .orElseThrow(),
            backup.getStatus().getSgBackupConfig()));

        Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getInitialData)
            .map(StackGresClusterInitialData::getRestore)
            .map(StackGresClusterRestore::getDownloadDiskConcurrency)
            .ifPresent(downloadDiskConcurrency -> data.put(
                "WALG_DOWNLOAD_CONCURRENCY",
                BackupStorageUtil.convertEnvValue(downloadDiskConcurrency)));
      }
    } else {
      data.put("RESTORE_BACKUP_ERROR", "Can not restore from backup. Backup not found!");
    }

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }

  private Map<String, String> getBackupEnvVars(
      StackGresClusterContext context,
      String path,
      StackGresBackupConfigSpec backupConfig) {
    Map<String, String> result = new HashMap<>(
        getBackupEnvVars(context,
            path,
            backupConfig.getStorage())
    );
    if (backupConfig.getBaseBackups() != null) {
      result.putAll(
          getBackupEnvVars(new BackupConfiguration(
              backupConfig.getBaseBackups().getRetention(),
              backupConfig.getBaseBackups().getCronSchedule(),
              backupConfig.getBaseBackups().getCompression(),
              path,
              Optional.of(backupConfig.getBaseBackups())
              .map(StackGresBaseBackupConfig::getPerformance)
              .map(p -> new BackupPerformance(
                  p.getMaxNetworkBandwidth(),
                  p.getMaxDiskBandwidth(),
                  p.getUploadDiskConcurrency(),
                  p.getUploadConcurrency(),
                  p.getDownloadConcurrency()))
              .orElse(null),
              Optional.of(context.getCluster().getSpec())
              .map(StackGresClusterSpec::getConfigurations)
              .map(StackGresClusterConfigurations::getBackups)
              .filter(Predicates.not(List::isEmpty))
              .map(backups -> backups.get(0))
              .map(StackGresClusterBackupConfiguration::getUseVolumeSnapshot)
              .orElse(false),
              null,
              null,
              null,
              null
          ))
      );
    }
    return Map.copyOf(result);
  }

  @Override
  protected String getAwsS3CompatibleCaCertificateFilePath(ClusterContext context) {
    return ClusterPath.RESTORE_SECRET_PATH.path(context)
        + "/" + BackupEnvVarFactory.AWS_S3_COMPATIBLE_CA_CERTIFICATE_FILE_NAME;
  }

  @Override
  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterPath.RESTORE_SECRET_PATH.path(context)
        + "/" + BackupEnvVarFactory.GCS_CREDENTIALS_FILE_NAME;
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
