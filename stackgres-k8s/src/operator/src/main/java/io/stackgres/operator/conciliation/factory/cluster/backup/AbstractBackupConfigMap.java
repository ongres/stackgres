/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.util.function.CheckedFunction;
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

    Optional.ofNullable(backupConfiguration)
        .map(BackupConfiguration::performance)
        .ifPresent(performance -> {
          Optional.ofNullable(performance.maxNetworkBandwitdh())
              .ifPresent(maxNetworkBandwitdh -> backupEnvVars.put(
                  "WALG_NETWORK_RATE_LIMIT", convertEnvValue(maxNetworkBandwitdh)));

          Optional.ofNullable(performance.maxDiskBandwitdh())
              .ifPresent(maxDiskBandwitdh -> backupEnvVars.put(
                  "WALG_DISK_RATE_LIMIT", convertEnvValue(maxDiskBandwitdh)));

          Optional.ofNullable(performance.uploadDiskConcurrency())
              .ifPresent(uploadDiskConcurrency -> backupEnvVars.put(
                  "WALG_UPLOAD_DISK_CONCURRENCY",
                  BackupStorageUtil.convertEnvValue(uploadDiskConcurrency)));
        });

    if (WAL_G_LOGGER.isTraceEnabled()) {
      backupEnvVars.put("WALG_LOG_LEVEL", "DEVEL");
    }

    return backupEnvVars.build();
  }

  protected Map<String, String> getBackupEnvVars(ClusterContext context,
                                                 String name, String namespace,
                                                 BackupStorage storage) {

    Optional<AwsS3Storage> storageForS3 = storage.getS3Opt();
    if (storageForS3.isPresent()) {
      return getS3StorageEnvVars(namespace, name, storageForS3.get());
    }

    Optional<AwsS3CompatibleStorage> storageForS3Compatible = storage.getS3CompatibleOpt();
    if (storageForS3Compatible.isPresent()) {
      return getS3CompatibleStorageEnvVars(
          namespace, name, storageForS3Compatible.get());
    }

    Optional<GoogleCloudStorage> storageForGcs = storage.getGcsOpt();
    if (storageForGcs.isPresent()) {
      return getGcsStorageEnvVars(context, namespace, name, storageForGcs.get());
    }

    Optional<AzureBlobStorage> storageForAzureBlob = storage.getAzureBlobOpt();
    return storageForAzureBlob.map(
        azureBlobStorage -> getAzureBlobStorageEnvVars(namespace, name, azureBlobStorage))
        .orElseGet(Map::of);

  }

  private Map<String, String> getS3StorageEnvVars(String namespace,
                                                  String name,
                                                  AwsS3Storage storageForS3) {
    return Map.of(
        "WALG_S3_PREFIX", getFromS3(storageForS3, AwsS3Storage::getPrefix)
            + "/" + namespace + "/" + name,
        "AWS_REGION", getFromS3(storageForS3, AwsS3Storage::getRegion),
        "WALG_S3_STORAGE_CLASS", getFromS3(storageForS3, AwsS3Storage::getStorageClass)
    );
  }

  private Map<String, String> getS3CompatibleStorageEnvVars(
      String namespace,
      String name,
      AwsS3CompatibleStorage storageForS3Compatible) {

    return Map.of(
        "WALG_S3_PREFIX", getFromS3Compatible(storageForS3Compatible,
            AwsS3CompatibleStorage::getPrefix)
            + "/" + namespace + "/" + name,
        "AWS_REGION", getFromS3Compatible(storageForS3Compatible,
            AwsS3CompatibleStorage::getRegion),
        "AWS_ENDPOINT", getFromS3Compatible(storageForS3Compatible,
            AwsS3CompatibleStorage::getEndpoint),
        "ENDPOINT_HOSTNAME", getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
            StackGresUtil::getHostFromUrl),
        "ENDPOINT_PORT", getFromS3Compatible(
            storageForS3Compatible, AwsS3CompatibleStorage::getEndpoint,
            StackGresUtil::getPortFromUrl),
        "AWS_S3_FORCE_PATH_STYLE", getFromS3Compatible(storageForS3Compatible,
            AwsS3CompatibleStorage::isForcePathStyle),
        "WALG_S3_STORAGE_CLASS", getFromS3Compatible(storageForS3Compatible,
            AwsS3CompatibleStorage::getStorageClass)
    );
  }

  private Map<String, String> getGcsStorageEnvVars(ClusterContext context,
                                                   String namespace, String name,
                                                   GoogleCloudStorage storageForGcs) {

    Map<String, String> backupEnvVars = new HashMap<>();
    backupEnvVars.put("WALG_GS_PREFIX", getFromGcs(storageForGcs, GoogleCloudStorage::getPrefix)
        + "/" + namespace + "/" + name);
    if (!Optional.of(storageForGcs)
        .map(GoogleCloudStorage::getCredentials)
        .map(GoogleCloudCredentials::isFetchCredentialsFromMetadataService)
        .orElse(false)) {
      backupEnvVars.put("GOOGLE_APPLICATION_CREDENTIALS", getGcsCredentialsFilePath(context));
    }
    return backupEnvVars;
  }

  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.BACKUP_SECRET_PATH.path(context)
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  private Map<String, String> getAzureBlobStorageEnvVars(String namespace, String name,
                                                         AzureBlobStorage storageForAzureBlob) {
    return Map.of("WALG_AZ_PREFIX", getFromAzureBlob(
        storageForAzureBlob, AzureBlobStorage::getPrefix)
        + "/" + namespace + "/" + name);
  }

  private <T> String getFromS3(AwsS3Storage storageFor,
                               Function<AwsS3Storage, T> getter) {
    return Optional.of(storageFor)
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String getFromS3Compatible(AwsS3CompatibleStorage storageFor,
                                         Function<AwsS3CompatibleStorage, T> getter) {
    return Optional.of(storageFor)
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T, R> String getFromS3Compatible(AwsS3CompatibleStorage storageFor,
                                            Function<AwsS3CompatibleStorage, T> getter,
                                            CheckedFunction<T, R> transformer) {
    return Optional.of(storageFor)
        .map(getter)
        .map(Unchecked.function(transformer))
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String getFromGcs(GoogleCloudStorage storageFor,
                                Function<GoogleCloudStorage, T> getter) {
    return Optional.of(storageFor)
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  private <T> String getFromAzureBlob(AzureBlobStorage storageFor,
                                      Function<AzureBlobStorage, T> getter) {
    return Optional.of(storageFor)
        .map(getter)
        .map(this::convertEnvValue)
        .orElse("");
  }

  protected <T> String convertEnvValue(T value) {
    return value.toString();
  }

}
