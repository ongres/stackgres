/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.storages.AwsS3Storage;
import io.stackgres.operator.customresource.storages.AzureBlobStorage;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.GoogleCloudStorage;
import io.stackgres.operator.sidecars.envoy.Envoy;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.util.function.CheckedFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBackupConfigMap {

  private static final Logger WAL_G_LOGGER = LoggerFactory.getLogger("wal-g");

  protected ImmutableMap<String, String> getBackupEnvVars(String namespace, String name,
      StackGresBackupConfigSpec backupConfigSpec) {
    ImmutableMap.Builder<String, String> backupEnvVars = ImmutableMap.builder();

    backupEnvVars.put("PGDATA", ClusterStatefulSetPath.PG_DATA_PATH.path());
    backupEnvVars.put("PGPORT", String.valueOf(Envoy.PG_RAW_PORT));
    backupEnvVars.put("PGUSER", "postgres");
    backupEnvVars.put("PGDATABASE", "postgres");
    backupEnvVars.put("PGHOST", ClusterStatefulSetPath.PG_RUN_PATH.path());

    backupEnvVars.put("WALG_COMPRESSION_METHOD", getFromConfigSpec(
        backupConfigSpec, StackGresBackupConfigSpec::getCompressionMethod));
    if (hasFromConfigSpec(backupConfigSpec, StackGresBackupConfigSpec::getNetworkRateLimit)) {
      backupEnvVars.put("WALG_NETWORK_RATE_LIMIT", getFromConfigSpec(
          backupConfigSpec, StackGresBackupConfigSpec::getNetworkRateLimit));
    }
    if (hasFromConfigSpec(backupConfigSpec, StackGresBackupConfigSpec::getDiskRateLimit)) {
      backupEnvVars.put("WALG_DISK_RATE_LIMIT", getFromConfigSpec(
          backupConfigSpec, StackGresBackupConfigSpec::getDiskRateLimit));
    }
    backupEnvVars.put("WALG_UPLOAD_DISK_CONCURRENCY", getFromConfigSpec(
        backupConfigSpec, StackGresBackupConfigSpec::getUploadDiskConcurrency));
    backupEnvVars.put("WALG_TAR_SIZE_THRESHOLD", getFromConfigSpec(
        backupConfigSpec, StackGresBackupConfigSpec::getTarSizeThreshold));

    Optional<AwsS3Storage> storageForS3 = getStorageFor(backupConfigSpec, BackupStorage::getS3);
    if (storageForS3.isPresent()) {
      setS3StorageEnvVars(namespace, name, backupEnvVars, storageForS3);
    }

    Optional<GoogleCloudStorage> storageForGcs = getStorageFor(
        backupConfigSpec, BackupStorage::getGcs);
    if (storageForGcs.isPresent()) {
      setGcsStorageEnvVars(namespace, name, backupEnvVars, storageForGcs);
    }

    Optional<AzureBlobStorage> storageForAzureBlob = getStorageFor(
        backupConfigSpec, BackupStorage::getAzureblob);
    if (storageForAzureBlob.isPresent()) {
      setAzureBlobStorageEnvVars(namespace, name, backupEnvVars, storageForAzureBlob);
    }

    if (WAL_G_LOGGER.isTraceEnabled()) {
      backupEnvVars.put("WALG_LOG_LEVEL", "DEVEL");
    }

    return backupEnvVars.build();
  }

  private void setS3StorageEnvVars(String namespace, String name,
      ImmutableMap.Builder<String, String> backupEnvVars, Optional<AwsS3Storage> storageForS3) {
    backupEnvVars.put("WALG_S3_PREFIX", getFromS3(storageForS3, AwsS3Storage::getPrefix)
        + "/" + namespace + "/" + name);
    backupEnvVars.put("AWS_REGION", getFromS3(storageForS3, AwsS3Storage::getRegion));
    backupEnvVars.put("AWS_ENDPOINT", getFromS3(storageForS3, AwsS3Storage::getEndpoint));
    backupEnvVars.put("ENDPOINT_HOSTNAME", getFromS3(
        storageForS3, AwsS3Storage::getEndpoint, StackGresUtil::getHostFromUrl));
    backupEnvVars.put("ENDPOINT_PORT", getFromS3(
        storageForS3, AwsS3Storage::getEndpoint, StackGresUtil::getPortFromUrl));
    backupEnvVars.put("AWS_S3_FORCE_PATH_STYLE", getFromS3(storageForS3,
        AwsS3Storage::isForcePathStyle));
    backupEnvVars.put("WALG_S3_STORAGE_CLASS", getFromS3(storageForS3,
        AwsS3Storage::getStorageClass));
    backupEnvVars.put("WALG_S3_SSE", getFromS3(storageForS3, AwsS3Storage::getSse));
    backupEnvVars.put("WALG_S3_SSE_KMS_ID", getFromS3(storageForS3, AwsS3Storage::getSseKmsId));
    backupEnvVars.put("WALG_CSE_KMS_ID", getFromS3(storageForS3, AwsS3Storage::getCseKmsId));
    backupEnvVars.put("WALG_CSE_KMS_REGION", getFromS3(storageForS3,
        AwsS3Storage::getCseKmsRegion));
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
    backupEnvVars.put("WALG_AZURE_BUFFER_SIZE", getFromAzureBlob(
        storageForAzureBlob, AzureBlobStorage::getBufferSize));
    backupEnvVars.put("WALG_AZURE_MAX_BUFFERS", getFromAzureBlob(
        storageForAzureBlob, AzureBlobStorage::getMaxBuffers));
  }

  protected String getGcsCredentialsFilePath() {
    return ClusterStatefulSetPath.BACKUP_SECRET_PATH.path()
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private <T> boolean hasFromConfigSpec(StackGresBackupConfigSpec configSpec,
      Function<StackGresBackupConfigSpec, T> getter) {
    return Optional.of(configSpec)
        .map(getter)
        .map(this::convertEnvValue)
        .isPresent();
  }

  private <T> String getFromConfigSpec(StackGresBackupConfigSpec configSpec,
      Function<StackGresBackupConfigSpec, T> getter) {
    return Optional.of(configSpec)
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
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

  private <T, R> String getFromS3(Optional<AwsS3Storage> storageFor,
      Function<AwsS3Storage, T> getter, CheckedFunction<T, R> transformer) {
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
