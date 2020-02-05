/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.QuarkusProfile;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.sgcluster.StackGresRestoreConfigSource;
import io.stackgres.operator.customresource.storages.AwsS3Storage;
import io.stackgres.operator.customresource.storages.AzureBlobStorage;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.GoogleCloudStorage;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniConfigMap {

  public static final String POSTGRES_PORT_NAME = "pgport";
  public static final String POSTGRES_REPLICATION_PORT_NAME = "pgreplication";

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("patroni");
  private static final Logger WAL_G_LOGGER = LoggerFactory.getLogger("wal-g");
  private static final String PATRONI_SUFFIX = "-patroni";
  private static final String BACKUP_SUFFIX = "-backup";
  private static final String RESTORE_SUFFIX = "-restore";

  private final PatroniRestoreSource patroniRestoreSource;

  @Inject
  public PatroniConfigMap(PatroniRestoreSource patroniRestoreSource) {
    this.patroniRestoreSource = patroniRestoreSource;
  }

  public static String patroniName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + PATRONI_SUFFIX);
  }

  public static String backupName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + BACKUP_SUFFIX);
  }

  public static String restoreName(StackGresClusterContext clusterContext) {
    return ResourceUtil.resourceName(clusterContext.getCluster().getMetadata().getName()
        + RESTORE_SUFFIX);
  }

  /**
   * Create the ConfigMaps associated with the cluster.
   */
  public List<ConfigMap> create(StackGresClusterContext context, ObjectMapper objectMapper) {
    return ImmutableList.of(
        createPatroniConfig(context, objectMapper),
        createBackupConfig(context),
        craeteRestoreConfig(context));
  }

  public ConfigMap createPatroniConfig(StackGresClusterContext context, ObjectMapper objectMapper) {
    final String name = patroniName(context);
    final String namespace = context.getCluster().getMetadata().getNamespace();
    Map<String, String> labels = ResourceUtil.patroniClusterLabels(context.getCluster());

    final String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getCluster().getSpec().getPostgresVersion());

    final String patroniLabels;
    try {
      patroniLabels = objectMapper.writeValueAsString(labels);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }

    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_SCOPE", ResourceUtil.clusterScope(context.getCluster()));
    data.put("PATRONI_KUBERNETES_SCOPE_LABEL", ResourceUtil.clusterScopeKey());
    data.put("PATRONI_KUBERNETES_LABELS", patroniLabels);
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_SUPERUSER_USERNAME", "postgres");
    data.put("PATRONI_REPLICATION_USERNAME", "replicator");
    data.put("PATRONI_POSTGRESQL_LISTEN", "127.0.0.1:" + Envoy.PG_RAW_PORT);
    data.put("PATRONI_POSTGRESQL_CONNECT_ADDRESS",
        "${PATRONI_KUBERNETES_POD_IP}:" + Envoy.PG_RAW_ENTRY_PORT);

    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:8008");
    data.put("PATRONI_POSTGRESQL_DATA_DIR", ClusterStatefulSet.DATA_VOLUME_PATH);
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", "/run/postgresql");

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    data.put("PGDATA", ClusterStatefulSet.DATA_VOLUME_PATH);
    data.put("PGPORT", String.valueOf(Envoy.PG_RAW_PORT));
    data.put("PGUSER", "postgres");
    data.put("PGDATABASE", "postgres");
    data.put("PGHOST", "/run/postgresql");

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(context.getCluster())))
        .endMetadata()
        .withData(data)
        .build();
  }

  private static ConfigMap createBackupConfig(StackGresClusterContext context) {
    final String name = backupName(context);
    final String namespace = context.getCluster().getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.patroniClusterLabels(context.getCluster());
    Map<String, String> data = new HashMap<>();

    context.getBackupConfig().ifPresent(backupConfig -> {
      data.put("BACKUP_CONFIG_RESOURCE_VERSION", backupConfig.getMetadata().getResourceVersion());
      data.put("WALG_COMPRESSION_METHOD", getFromConfig(
          backupConfig, StackGresBackupConfigSpec::getCompressionMethod));
      if (hasFromConfig(backupConfig, StackGresBackupConfigSpec::getNetworkRateLimit)) {
        data.put("WALG_NETWORK_RATE_LIMIT", getFromConfig(
            backupConfig, StackGresBackupConfigSpec::getNetworkRateLimit));
      }
      if (hasFromConfig(backupConfig, StackGresBackupConfigSpec::getDiskRateLimit)) {
        data.put("WALG_DISK_RATE_LIMIT", getFromConfig(
            backupConfig, StackGresBackupConfigSpec::getDiskRateLimit));
      }
      data.put("WALG_UPLOAD_DISK_CONCURRENCY", getFromConfig(
          backupConfig, StackGresBackupConfigSpec::getUploadDiskConcurrency));
      data.put("WALG_TAR_SIZE_THRESHOLD", getFromConfig(
          backupConfig, StackGresBackupConfigSpec::getTarSizeThreshold));

      Optional<AwsS3Storage> storageForS3 = getStorageFor(backupConfig, BackupStorage::getS3);
      if (storageForS3.isPresent()) {
        data.put("WALG_S3_PREFIX", getFromS3(storageForS3, AwsS3Storage::getPrefix)
            + "/" + namespace + "/" + name);
        data.put("AWS_REGION", getFromS3(storageForS3, AwsS3Storage::getRegion));
        data.put("AWS_ENDPOINT", getFromS3(storageForS3, AwsS3Storage::getEndpoint));
        data.put("AWS_S3_FORCE_PATH_STYLE", getFromS3(storageForS3,
            AwsS3Storage::isForcePathStyle));
        data.put("WALG_S3_STORAGE_CLASS", getFromS3(storageForS3, AwsS3Storage::getStorageClass));
        data.put("WALG_S3_SSE", getFromS3(storageForS3, AwsS3Storage::getSse));
        data.put("WALG_S3_SSE_KMS_ID", getFromS3(storageForS3, AwsS3Storage::getSseKmsId));
        data.put("WALG_CSE_KMS_ID", getFromS3(storageForS3, AwsS3Storage::getCseKmsId));
        data.put("WALG_CSE_KMS_REGION", getFromS3(storageForS3, AwsS3Storage::getCseKmsRegion));
      }

      Optional<GoogleCloudStorage> storageForGcs = getStorageFor(
          backupConfig, BackupStorage::getGcs);
      if (storageForGcs.isPresent()) {
        data.put("WALG_GCS_PREFIX", getFromGcs(storageForGcs, GoogleCloudStorage::getPrefix)
            + "/" + namespace + "/" + name);
      }

      Optional<AzureBlobStorage> storageForAzureBlob = getStorageFor(
          backupConfig, BackupStorage::getAzureblob);
      if (storageForAzureBlob.isPresent()) {
        data.put("WALG_AZ_PREFIX", getFromAzureBlob(
            storageForAzureBlob, AzureBlobStorage::getPrefix)
            + "/" + namespace + "/" + name);
        data.put("WALG_AZURE_BUFFER_SIZE", getFromAzureBlob(
            storageForAzureBlob, AzureBlobStorage::getBufferSize));
        data.put("WALG_AZURE_MAX_BUFFERS", getFromAzureBlob(
            storageForAzureBlob, AzureBlobStorage::getMaxBuffers));
      }

      if (WAL_G_LOGGER.isTraceEnabled()) {
        data.put("WALG_LOG_LEVEL", "DEVEL");
      }
    });

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getCluster())))
        .endMetadata()
        .withData(data)
        .build();
  }

  private ConfigMap craeteRestoreConfig(StackGresClusterContext context) {
    final String name = restoreName(context);
    final String namespace = context.getCluster().getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.patroniClusterLabels(context.getCluster());
    Map<String, String> data = new HashMap<>();

    context.getRestoreConfig().ifPresent(restoreConfig -> {
      putIfPresent("WALG_DOWNLOAD_CONCURRENCY",
          restoreConfig.getDownloadDiskConcurrency(), data);

      StackGresRestoreConfigSource source = patroniRestoreSource.getStorageConfig(restoreConfig);

      putIfPresent("WALG_COMPRESSION_METHOD", source.getCompressionMethod(), data);

      putIfPresent("BACKUP_ID", source.getBackupName(), data);

      BackupStorage storage = source.getStorage();

      Optional.ofNullable(storage.getS3()).ifPresent(s3config -> {
        data.put("WALG_S3_PREFIX", s3config.getPrefix());
        putIfPresent("AWS_REGION", s3config.getRegion(), data);
        putIfPresent("AWS_ENDPOINT", s3config.getEndpoint(), data);
        putIfPresent("AWS_S3_FORCE_PATH_STYLE", s3config.isForcePathStyle(), data);
        putIfPresent("WALG_S3_STORAGE_CLASS", s3config.getStorageClass(), data);
        putIfPresent("WALG_S3_SSE", s3config.getSse(), data);
        putIfPresent("WALG_S3_SSE_KMS_ID", s3config.getSseKmsId(), data);
        putIfPresent("WALG_CSE_KMS_ID", s3config.getCseKmsId(), data);
        putIfPresent("WALG_CSE_KMS_REGION", s3config.getCseKmsRegion(), data);
      });

      Optional.ofNullable(storage.getGcs()).ifPresent(gcsConfig -> {
        Optional.ofNullable(storage.getGcs())
            .ifPresent(volume ->
                data.put("WALG_GCS_PREFIX", gcsConfig.getPrefix()));
      });

      Optional.ofNullable(storage.getAzureblob()).ifPresent(azureConfig -> {
        Optional.ofNullable(storage.getAzureblob())
            .ifPresent(volume ->
                data.put("WALG_AZ_PREFIX", azureConfig.getPrefix()));

        putIfPresent("WALG_AZURE_BUFFER_SIZE", azureConfig.getBufferSize(), data);
        putIfPresent("WALG_AZURE_MAX_BUFFERS", azureConfig.getMaxBuffers(), data);
      });

      if (WAL_G_LOGGER.isTraceEnabled() || QuarkusProfile.getActiveProfile().isDev()) {
        data.put("WALG_LOG_LEVEL", "DEVEL");
      }
    });

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getCluster())))
        .endMetadata()
        .withData(data)
        .build();
  }

  private static <T> void putIfPresent(String env, T p, Map<String, String> data) {
    Optional.ofNullable(p).ifPresent(value -> data.put(env, value.toString()));
  }

  private static <T> boolean hasFromConfig(StackGresBackupConfig config,
      Function<StackGresBackupConfigSpec, T> getter) {
    return Optional.of(config)
        .map(StackGresBackupConfig::getSpec)
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .isPresent();
  }

  private static <T> String getFromConfig(StackGresBackupConfig config,
      Function<StackGresBackupConfigSpec, T> getter) {
    return Optional.of(config)
        .map(StackGresBackupConfig::getSpec)
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .orElse("");
  }

  private static <T> Optional<T> getStorageFor(StackGresBackupConfig config,
      Function<BackupStorage, T> getter) {
    return Optional.of(config)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(getter);
  }

  private static <T> String getFromS3(Optional<AwsS3Storage> storageFor,
      Function<AwsS3Storage, T> getter) {
    return storageFor
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .orElse("");
  }

  private static <T> String getFromGcs(Optional<GoogleCloudStorage> storageFor,
      Function<GoogleCloudStorage, T> getter) {
    return storageFor
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .orElse("");
  }

  private static <T> String getFromAzureBlob(Optional<AzureBlobStorage> storageFor,
      Function<AzureBlobStorage, T> getter) {
    return storageFor
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .orElse("");
  }

  private static <T> String convertEnvValue(T value) {
    return value.toString();
  }

}
