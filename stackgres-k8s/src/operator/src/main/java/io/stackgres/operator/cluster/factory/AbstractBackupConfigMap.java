/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.util.function.CheckedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBackupConfigMap {

  private static final Logger WAL_G_LOGGER = LoggerFactory.getLogger("wal-g");

  protected ImmutableMap<String, String> getBackupEnvVars(
      String namespace, String name,
      StackGresBackupConfigSpec backupConfigSpec) {
    ImmutableMap.Builder<String, String> backupEnvVars = ImmutableMap.builder();

    backupEnvVars.put("PGDATA", ClusterStatefulSetPath.PG_DATA_PATH.path());
    backupEnvVars.put("PGPORT", String.valueOf(Envoy.PG_RAW_PORT));
    backupEnvVars.put("PGUSER", "postgres");
    backupEnvVars.put("PGDATABASE", "postgres");
    backupEnvVars.put("PGHOST", ClusterStatefulSetPath.PG_RUN_PATH.path());

    Optional.ofNullable(backupConfigSpec.getBaseBackups())
        .map(StackGresBaseBackupConfig::getCompression)
        .ifPresent(compression -> backupEnvVars.put("WALG_COMPRESSION_METHOD", compression));

    Optional.ofNullable(backupConfigSpec.getBaseBackups())
        .map(StackGresBaseBackupConfig::getPerformance)
        .ifPresent(performance -> {
          final long networkRateLimit = performance.getMaxNetworkBandwitdh();
          backupEnvVars.put("WALG_NETWORK_RATE_LIMIT", convertEnvValue(networkRateLimit));

          final long diskRateLimit = performance.getMaxDiskBandwitdh();
          backupEnvVars.put("WALG_DISK_RATE_LIMIT", convertEnvValue(diskRateLimit));

          final int uploadDiskConcurrency = performance.getUploadDiskConcurrency();
          backupEnvVars.put("WALG_UPLOAD_DISK_CONCURRENCY",
                convertEnvValue(uploadDiskConcurrency));
        });

    Optional<AwsS3Storage> storageForS3 = getStorageFor(backupConfigSpec, BackupStorage::getS3);
    if (storageForS3.isPresent()) {
      setS3StorageEnvVars(namespace, name, backupEnvVars, storageForS3);
    }

    Optional<AwsS3CompatibleStorage> storageForS3Compatible = getStorageFor(backupConfigSpec,
        BackupStorage::getS3Compatible);
    if (storageForS3Compatible.isPresent()) {
      setS3CompatibleStorageEnvVars(namespace, name, backupEnvVars, storageForS3Compatible);
    }

    Optional<GoogleCloudStorage> storageForGcs = getStorageFor(
        backupConfigSpec, BackupStorage::getGcs);
    if (storageForGcs.isPresent()) {
      setGcsStorageEnvVars(namespace, name, backupEnvVars, storageForGcs);
    }

    Optional<AzureBlobStorage> storageForAzureBlob = getStorageFor(
        backupConfigSpec, BackupStorage::getAzureBlob);
    if (storageForAzureBlob.isPresent()) {
      setAzureBlobStorageEnvVars(namespace, name, backupEnvVars, storageForAzureBlob);
    }

    if (WAL_G_LOGGER.isTraceEnabled()) {
      backupEnvVars.put("WALG_LOG_LEVEL", "DEVEL");
    }

    return backupEnvVars.build();
  }

  private void setS3StorageEnvVars(String namespace,
                                   String name,
                                   ImmutableMap.Builder<String, String> backupEnvVars,
                                   Optional<AwsS3Storage> storageForS3) {
    backupEnvVars.put("WALG_S3_PREFIX", getFromS3(storageForS3, AwsS3Storage::getPrefix)
        + "/" + namespace + "/" + name);
    backupEnvVars.put("AWS_REGION", getFromS3(storageForS3, AwsS3Storage::getRegion));
    backupEnvVars.put("WALG_S3_STORAGE_CLASS", getFromS3(storageForS3,
        AwsS3Storage::getStorageClass));
  }

  private void setS3CompatibleStorageEnvVars(
      String namespace,
      String name,
      ImmutableMap.Builder<String, String> backupEnvVars,
      Optional<AwsS3CompatibleStorage> storageForS3Compatible) {
    backupEnvVars.put("WALG_S3_PREFIX", getFromS3Compatible(storageForS3Compatible,
        AwsS3CompatibleStorage::getPrefix)
        + "/" + namespace + "/" + name);
    backupEnvVars.put("AWS_REGION", getFromS3Compatible(storageForS3Compatible,
        AwsS3CompatibleStorage::getRegion));
    backupEnvVars.put("AWS_ENDPOINT", getFromS3Compatible(storageForS3Compatible,
        AwsS3CompatibleStorage::getEndpoint));
    backupEnvVars.put("ENDPOINT_HOSTNAME", getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
        StackGresUtil::getHostFromUrl));
    backupEnvVars.put("ENDPOINT_PORT", getFromS3Compatible(
        storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
        StackGresUtil::getPortFromUrl));
    backupEnvVars.put("AWS_S3_FORCE_PATH_STYLE", getFromS3Compatible(storageForS3Compatible,
        AwsS3CompatibleStorage::isForcePathStyle));
    backupEnvVars.put("WALG_S3_STORAGE_CLASS", getFromS3Compatible(storageForS3Compatible,
        AwsS3CompatibleStorage::getStorageClass));
  }

  private void setGcsStorageEnvVars(String namespace, String name,
                                    ImmutableMap.Builder<String, String> backupEnvVars,
                                    Optional<GoogleCloudStorage> storageForGcs) {
    backupEnvVars.put("WALG_GCS_PREFIX", getFromGcs(storageForGcs, GoogleCloudStorage::getPrefix)
        + "/" + namespace + "/" + name);
    backupEnvVars.put("GOOGLE_APPLICATION_CREDENTIALS", getGcsCredentialsFilePath());
  }

  private void setAzureBlobStorageEnvVars(String namespace, String name,
                                          ImmutableMap.Builder<String, String> backupEnvVars,
                                          Optional<AzureBlobStorage> storageForAzureBlob) {
    backupEnvVars.put("WALG_AZ_PREFIX", getFromAzureBlob(
        storageForAzureBlob, AzureBlobStorage::getPrefix)
        + "/" + namespace + "/" + name);
  }

  protected String getGcsCredentialsFilePath() {
    return ClusterStatefulSetPath.BACKUP_SECRET_PATH.path()
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private <T> Optional<T> getStorageFor(StackGresBackupConfigSpec configSpec,
                                        Function<BackupStorage, T> getter) {
    return Optional.of(configSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(getter);
  }

  private <T> String getFromS3(Optional<AwsS3Storage> storageFor,
                               Function<AwsS3Storage, T> getter) {
    return storageFor
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String getFromS3Compatible(Optional<AwsS3CompatibleStorage> storageFor,
                                         Function<AwsS3CompatibleStorage, T> getter) {
    return storageFor
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T, R> String getFromS3Compatible(Optional<AwsS3CompatibleStorage> storageFor,
                                            Function<AwsS3CompatibleStorage, T> getter,
                                            CheckedFunction<T, R> transformer) {
    return storageFor
        .map(getter)
        .map(Unchecked.function(transformer))
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String getFromGcs(Optional<GoogleCloudStorage> storageFor,
                                Function<GoogleCloudStorage, T> getter) {
    return storageFor
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String getFromAzureBlob(Optional<AzureBlobStorage> storageFor,
                                      Function<AzureBlobStorage, T> getter) {
    return storageFor
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String convertEnvValue(T value) {
    return value.toString();
  }

}
