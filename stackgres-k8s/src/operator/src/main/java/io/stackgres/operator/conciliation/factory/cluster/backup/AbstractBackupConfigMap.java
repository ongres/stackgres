/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBackupConfigMap {

  private static final Logger WAL_G_LOGGER = LoggerFactory.getLogger("io.stackgres.wal-g");

  protected ImmutableMap<String, String> getBackupEnvVars(
      ClusterContext context,
      String path,
      StackGresBackupConfigSpec backupConfigSpec) {
    ImmutableMap.Builder<String, String> backupEnvVars = ImmutableMap.builder();

    backupEnvVars.put("PGDATA", ClusterStatefulSetPath.PG_DATA_PATH.path());
    backupEnvVars.put("PGPORT", String.valueOf(EnvoyUtil.PG_PORT));
    backupEnvVars.put("PGUSER", "postgres");
    backupEnvVars.put("PGDATABASE", "postgres");
    backupEnvVars.put("PGHOST", ClusterStatefulSetPath.PG_RUN_PATH.path());

    Optional.ofNullable(backupConfigSpec.getBaseBackups())
        .map(StackGresBaseBackupConfig::getCompression)
        .ifPresent(compression -> backupEnvVars.put("WALG_COMPRESSION_METHOD", compression));

    Optional.ofNullable(backupConfigSpec.getBaseBackups())
        .map(StackGresBaseBackupConfig::getPerformance)
        .ifPresent(performance -> {
          Optional.ofNullable(performance.getMaxNetworkBandwitdh())
              .ifPresent(maxNetworkBandwitdh -> backupEnvVars.put(
                  "WALG_NETWORK_RATE_LIMIT",
                  BackupStorageUtil.convertEnvValue(maxNetworkBandwitdh)));

          Optional.ofNullable(performance.getMaxDiskBandwitdh())
              .ifPresent(maxDiskBandwitdh -> backupEnvVars.put(
                  "WALG_DISK_RATE_LIMIT",
                  BackupStorageUtil.convertEnvValue(maxDiskBandwitdh)));

          Optional.ofNullable(performance.getUploadDiskConcurrency())
              .ifPresent(uploadDiskConcurrency -> backupEnvVars.put(
                  "WALG_UPLOAD_DISK_CONCURRENCY",
                  BackupStorageUtil.convertEnvValue(uploadDiskConcurrency)));
        });

    Optional<AwsS3Storage> storageForS3 = BackupStorageUtil.getStorageFor(
        backupConfigSpec, BackupStorage::getS3);
    if (storageForS3.isPresent()) {
      setS3StorageEnvVars(
          path, backupEnvVars, storageForS3);
    }

    Optional<AwsS3CompatibleStorage> storageForS3Compatible = BackupStorageUtil.getStorageFor(
        backupConfigSpec, BackupStorage::getS3Compatible);
    if (storageForS3Compatible.isPresent()) {
      setS3CompatibleStorageEnvVars(
          path, backupEnvVars, storageForS3Compatible);
    }

    Optional<GoogleCloudStorage> storageForGcs = BackupStorageUtil.getStorageFor(
        backupConfigSpec, BackupStorage::getGcs);
    if (storageForGcs.isPresent()) {
      setGcsStorageEnvVars(
          context, path, backupEnvVars, storageForGcs);
    }

    Optional<AzureBlobStorage> storageForAzureBlob = BackupStorageUtil.getStorageFor(
        backupConfigSpec, BackupStorage::getAzureBlob);
    if (storageForAzureBlob.isPresent()) {
      setAzureBlobStorageEnvVars(
          path, backupEnvVars, storageForAzureBlob);
    }

    if (WAL_G_LOGGER.isTraceEnabled()) {
      backupEnvVars.put("WALG_LOG_LEVEL", "DEVEL");
    }

    return backupEnvVars.build();
  }

  private void setS3StorageEnvVars(
      String path,
      ImmutableMap.Builder<String, String> backupEnvVars,
      Optional<AwsS3Storage> storageForS3) {
    backupEnvVars.put("WALG_S3_PREFIX", BackupStorageUtil.getPrefixForS3(
        path, storageForS3));
    backupEnvVars.put("AWS_REGION", BackupStorageUtil.getFromS3(
        storageForS3, AwsS3Storage::getRegion));
    backupEnvVars.put("WALG_S3_STORAGE_CLASS", BackupStorageUtil.getFromS3(
        storageForS3, AwsS3Storage::getStorageClass));
  }

  private void setS3CompatibleStorageEnvVars(
      String path,
      ImmutableMap.Builder<String, String> backupEnvVars,
      Optional<AwsS3CompatibleStorage> storageForS3Compatible) {
    backupEnvVars.put("WALG_S3_PREFIX", BackupStorageUtil.getPrefixForS3Compatible(
        path, storageForS3Compatible));
    backupEnvVars.put("AWS_REGION", BackupStorageUtil.getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getRegion));
    backupEnvVars.put("AWS_ENDPOINT", BackupStorageUtil.getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint));
    backupEnvVars.put("ENDPOINT_HOSTNAME", BackupStorageUtil.getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
        StackGresUtil::getHostFromUrl));
    backupEnvVars.put("ENDPOINT_PORT", BackupStorageUtil.getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
        StackGresUtil::getPortFromUrl));
    backupEnvVars.put("AWS_S3_FORCE_PATH_STYLE", BackupStorageUtil.getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::isForcePathStyle));
    backupEnvVars.put("WALG_S3_STORAGE_CLASS", BackupStorageUtil.getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getStorageClass));
  }

  private void setGcsStorageEnvVars(
      ClusterContext context,
      String path,
      ImmutableMap.Builder<String, String> backupEnvVars,
      Optional<GoogleCloudStorage> storageForGcs) {
    backupEnvVars.put("WALG_GS_PREFIX", BackupStorageUtil.getPrefixForGcs(
        path, storageForGcs));
    if (!storageForGcs
        .map(GoogleCloudStorage::getCredentials)
        .map(GoogleCloudCredentials::isFetchCredentialsFromMetadataService)
        .orElse(false)) {
      backupEnvVars.put("GOOGLE_APPLICATION_CREDENTIALS", getGcsCredentialsFilePath(context));
    }
  }

  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.BACKUP_SECRET_PATH.path(context)
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private void setAzureBlobStorageEnvVars(
      String path,
      ImmutableMap.Builder<String, String> backupEnvVars,
      Optional<AzureBlobStorage> storageForAzureBlob) {
    backupEnvVars.put("WALG_AZ_PREFIX", BackupStorageUtil.getPrefixForAzureBlob(
        path, storageForAzureBlob));
  }

}
