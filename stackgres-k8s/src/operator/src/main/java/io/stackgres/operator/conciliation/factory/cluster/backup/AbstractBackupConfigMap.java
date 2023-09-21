/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBackupConfigMap {

  private static final Logger WAL_G_LOGGER = LoggerFactory.getLogger("io.stackgres.wal-g");

  protected ImmutableMap<String, String> getBackupEnvVars(
      BackupConfiguration backupConfiguration) {
    ImmutableMap.Builder<String, String> backupEnvVars = ImmutableMap.builder();

    backupEnvVars.put("PGDATA", ClusterStatefulSetPath.PG_DATA_PATH.path());
    backupEnvVars.put("PGPORT", String.valueOf(EnvoyUtil.PG_PORT));
    backupEnvVars.put("PGUSER", "postgres");
    backupEnvVars.put("PGDATABASE", "postgres");
    backupEnvVars.put("PGHOST", ClusterStatefulSetPath.PG_RUN_PATH.path());

    Optional.ofNullable(backupConfiguration)
        .map(BackupConfiguration::compression)
        .ifPresent(compression -> backupEnvVars.put("WALG_COMPRESSION_METHOD", compression));

    Optional<BackupPerformance> performance = Optional.ofNullable(backupConfiguration)
        .map(BackupConfiguration::performance);

    performance.map(BackupPerformance::maxNetworkBandwidth)
        .ifPresent(maxNetworkBandwidth -> backupEnvVars.put(
            "WALG_NETWORK_RATE_LIMIT",
            BackupStorageUtil.convertEnvValue(maxNetworkBandwidth)));

    performance.map(BackupPerformance::maxDiskBandwidth)
        .ifPresent(maxDiskBandwidth -> backupEnvVars.put(
            "WALG_DISK_RATE_LIMIT",
            BackupStorageUtil.convertEnvValue(maxDiskBandwidth)));

    performance.map(BackupPerformance::uploadDiskConcurrency)
        .ifPresent(uploadDiskConcurrency -> backupEnvVars.put(
            "WALG_UPLOAD_DISK_CONCURRENCY",
            BackupStorageUtil.convertEnvValue(uploadDiskConcurrency)));

    performance.map(BackupPerformance::uploadConcurrency)
        .ifPresent(uploadDiskConcurrency -> backupEnvVars.put(
            "WALG_UPLOAD_CONCURRENCY",
            BackupStorageUtil.convertEnvValue(uploadDiskConcurrency)));

    performance.map(BackupPerformance::downloadConcurrency)
        .ifPresent(uploadDiskConcurrency -> backupEnvVars.put(
            "WALG_DOWNLOAD_CONCURRENCY",
            BackupStorageUtil.convertEnvValue(uploadDiskConcurrency)));

    if (WAL_G_LOGGER.isTraceEnabled()) {
      backupEnvVars.put("WALG_LOG_LEVEL", "DEVEL");
    }

    return backupEnvVars.build();
  }

  protected Map<String, String> getBackupEnvVars(ClusterContext context,
      String path,
      BackupStorage storage) {

    Optional<AwsS3Storage> storageForS3 = storage.getS3Opt();
    if (storageForS3.isPresent()) {
      return getS3StorageEnvVars(path, storageForS3.get());
    }

    Optional<AwsS3CompatibleStorage> storageForS3Compatible = storage.getS3CompatibleOpt();
    if (storageForS3Compatible.isPresent()) {
      return getS3CompatibleStorageEnvVars(
          context, path, storageForS3Compatible.get());
    }

    Optional<GoogleCloudStorage> storageForGcs = storage.getGcsOpt();
    if (storageForGcs.isPresent()) {
      return getGcsStorageEnvVars(context, path, storageForGcs.get());
    }

    Optional<AzureBlobStorage> storageForAzureBlob = storage.getAzureBlobOpt();
    return storageForAzureBlob.map(
        azureBlobStorage -> getAzureBlobStorageEnvVars(path, azureBlobStorage))
        .orElseGet(Map::of);

  }

  private Map<String, String> getS3StorageEnvVars(
      String path,
      AwsS3Storage storageForS3) {

    return Map.of(
        "WALG_S3_PREFIX", BackupStorageUtil.getPrefixForS3(path, storageForS3),
        "AWS_REGION", BackupStorageUtil.getFromS3(
            storageForS3, AwsS3Storage::getRegion),
        "WALG_S3_STORAGE_CLASS", BackupStorageUtil.getFromS3(
            storageForS3, AwsS3Storage::getStorageClass));
  }

  private Map<String, String> getS3CompatibleStorageEnvVars(
      ClusterContext context,
      String path,
      AwsS3CompatibleStorage storageForS3Compatible) {

    Map<String, String> backupEnvVars = new HashMap<>();
    backupEnvVars.putAll(Map.of(
        "WALG_S3_PREFIX", BackupStorageUtil
            .getPrefixForS3Compatible(path, storageForS3Compatible),
        "AWS_REGION", BackupStorageUtil.getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getRegion),
        "AWS_ENDPOINT", BackupStorageUtil.getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint),
        "ENDPOINT_HOSTNAME", BackupStorageUtil.getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
            StackGresUtil::getHostFromUrl),
        "ENDPOINT_PORT", BackupStorageUtil.getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
            StackGresUtil::getPortFromUrl),
        "AWS_S3_FORCE_PATH_STYLE", BackupStorageUtil.getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::isEnablePathStyleAddressing),
        "WALG_S3_STORAGE_CLASS", BackupStorageUtil.getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getStorageClass)));
    if (Optional.of(storageForS3Compatible)
        .map(AwsS3CompatibleStorage::getAwsCredentials)
        .map(AwsCredentials::getSecretKeySelectors)
        .map(AwsSecretKeySelector::getCaCertificate)
        .isPresent()) {
      backupEnvVars.put("WALG_S3_CA_CERT_FILE", getAwsS3CompatibleCaCertificateFilePath(context));
    }
    return backupEnvVars;
  }

  protected String getAwsS3CompatibleCaCertificateFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.BACKUP_SECRET_PATH.path(context)
        + "/" + BackupEnvVarFactory.AWS_S3_COMPATIBLE_CA_CERTIFICATE_FILE_NAME;
  }

  private Map<String, String> getGcsStorageEnvVars(
      ClusterContext context,
      String path,
      GoogleCloudStorage storageForGcs) {

    Map<String, String> backupEnvVars = new HashMap<>();
    backupEnvVars.put("WALG_GS_PREFIX", BackupStorageUtil.getPrefixForGcs(
        path, storageForGcs));
    if (!Optional.of(storageForGcs)
        .map(GoogleCloudStorage::getGcpCredentials)
        .map(GoogleCloudCredentials::isFetchCredentialsFromMetadataService)
        .orElse(false)) {
      backupEnvVars.put("GOOGLE_APPLICATION_CREDENTIALS", getGcsCredentialsFilePath(context));
    }
    return backupEnvVars;
  }

  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.BACKUP_SECRET_PATH.path(context)
        + "/" + BackupEnvVarFactory.GCS_CREDENTIALS_FILE_NAME;
  }

  private Map<String, String> getAzureBlobStorageEnvVars(String path,
      AzureBlobStorage storageForAzureBlob) {
    return Map.of("WALG_AZ_PREFIX", BackupStorageUtil.getPrefixForAzureBlob(
        path, storageForAzureBlob));
  }

}
