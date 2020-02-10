/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
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

  private PatroniRestoreSource patroniRestoreSource;

  @Inject
  public PatroniConfigMap(PatroniRestoreSource patroniRestoreSource) {
    this.patroniRestoreSource = patroniRestoreSource;
  }

  /**
   * Create the ConfigMap associated with the cluster.
   */
  public ConfigMap create(StackGresClusterContext context, ObjectMapper objectMapper) {
    final String name = context.getCluster().getMetadata().getName();
    final String namespace = context.getCluster().getMetadata().getNamespace();
    final String pgVersion = StackGresComponents.calculatePostgresVersion(
        context.getCluster().getSpec().getPostgresVersion());

    Map<String, String> labels = ResourceUtil.patroniClusterLabels(context.getCluster());

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

    putBackupConfigs(context, data);
    putRestoreConfigs(context, data);

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

  private void putRestoreConfigs(StackGresClusterContext config,
                                 Map<String, String> data) {

    config.getRestoreConfig().ifPresent(restoreConfig -> {

      putIfPresent("RESTORE_WALG_DOWNLOAD_CONCURRENCY",
          restoreConfig.getDownloadDiskConcurrency(), data);

      StackGresRestoreConfigSource source = patroniRestoreSource.getStorageConfig(restoreConfig);

      putIfPresent("RESTORE_WALG_COMPRESSION_METHOD", source.getCompressionMethod(), data);

      putIfPresent("RESTORE_BACKUP_ID", source.getBackupName(), data);

      BackupStorage storage = source.getStorage();

      Optional.ofNullable(storage.getS3()).ifPresent(s3config -> {

        data.put("RESTORE_WALG_S3_PREFIX", s3config.getPrefix());
        putIfPresent("RESTORE_AWS_REGION", s3config.getRegion(), data);
        Optional.ofNullable(s3config.getEndpoint()).ifPresent(endpoint -> {
          data.put("RESTORE_AWS_ENDPOINT", endpoint);

          try {
            String host = getHostFromUrl(endpoint);
            int port = getPortFromUrl(endpoint);
            data.put("RESTORE_ENDPOINT_HOSTNAME", host);
            data.put("RESTORE_ENDPOINT_PORT", Integer.toString(port));
          } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException("Malformed S3 endpoint", e);
          }
        });

        putIfPresent("RESTORE_AWS_S3_FORCE_PATH_STYLE", s3config.isForcePathStyle(), data);
        putIfPresent("RESTORE_WALG_S3_STORAGE_CLASS", s3config.getStorageClass(), data);
        putIfPresent("RESTORE_WALG_S3_SSE", s3config.getSse(), data);
        putIfPresent("RESTORE_WALG_S3_SSE_KMS_ID", s3config.getSseKmsId(), data);
        putIfPresent("RESTORE_WALG_CSE_KMS_ID", s3config.getCseKmsId(), data);
        putIfPresent("RESTORE_WALG_CSE_KMS_REGION", s3config.getCseKmsRegion(), data);

      });

      Optional.ofNullable(storage.getGcs()).ifPresent(gcsConfig -> {
        Optional.ofNullable(storage.getGcs())
            .ifPresent(volume ->
                data.put("RESTORE_WALG_GS_PREFIX", gcsConfig.getPrefix()));
      });

      Optional.ofNullable(storage.getAzureblob()).ifPresent(azureConfig -> {

        Optional.ofNullable(storage.getAzureblob())
            .ifPresent(volume ->
                data.put("RESTORE_WALG_AZ_PREFIX", azureConfig.getPrefix()));

        putIfPresent("RESTORE_WALG_AZURE_BUFFER_SIZE", azureConfig.getBufferSize(), data);
        putIfPresent("RESTORE_WALG_AZURE_MAX_BUFFERS", azureConfig.getMaxBuffers(), data);

      });

      if (WAL_G_LOGGER.isTraceEnabled() || QuarkusProfile.getActiveProfile().isDev()) {
        data.put("RESTORE_WALG_LOG_LEVEL", "DEVEL");
      }

    });

  }

  private static void putBackupConfigs(StackGresClusterContext context,
                                       Map<String, String> data) {

    final String name = context.getCluster().getMetadata().getName();
    final String namespace = context.getCluster().getMetadata().getNamespace();

    data.put("WALG_COMPRESSION_METHOD", getFromConfig(
        context, StackGresBackupConfigSpec::getCompressionMethod));
    if (hasFromConfig(context, StackGresBackupConfigSpec::getNetworkRateLimit)) {
      data.put("WALG_NETWORK_RATE_LIMIT", getFromConfig(
          context, StackGresBackupConfigSpec::getNetworkRateLimit));
    }
    if (hasFromConfig(context, StackGresBackupConfigSpec::getDiskRateLimit)) {
      data.put("WALG_DISK_RATE_LIMIT", getFromConfig(
          context, StackGresBackupConfigSpec::getDiskRateLimit));
    }
    data.put("WALG_UPLOAD_DISK_CONCURRENCY", getFromConfig(
        context, StackGresBackupConfigSpec::getUploadDiskConcurrency));
    data.put("WALG_TAR_SIZE_THRESHOLD", getFromConfig(
        context, StackGresBackupConfigSpec::getTarSizeThreshold));

    Optional<AwsS3Storage> storageForS3 = getStorageFor(context, BackupStorage::getS3);
    if (storageForS3.isPresent()) {
      data.put("WALG_S3_PREFIX", getFromS3(storageForS3, AwsS3Storage::getPrefix)
          + "/" + namespace + "/" + name);
      data.put("AWS_REGION", getFromS3(storageForS3, AwsS3Storage::getRegion));
      data.put("AWS_ENDPOINT", getFromS3(storageForS3, AwsS3Storage::getEndpoint));
      data.put("AWS_S3_FORCE_PATH_STYLE", getFromS3(storageForS3, AwsS3Storage::isForcePathStyle));
      data.put("WALG_S3_STORAGE_CLASS", getFromS3(storageForS3, AwsS3Storage::getStorageClass));
      data.put("WALG_S3_SSE", getFromS3(storageForS3, AwsS3Storage::getSse));
      data.put("WALG_S3_SSE_KMS_ID", getFromS3(storageForS3, AwsS3Storage::getSseKmsId));
      data.put("WALG_CSE_KMS_ID", getFromS3(storageForS3, AwsS3Storage::getCseKmsId));
      data.put("WALG_CSE_KMS_REGION", getFromS3(storageForS3, AwsS3Storage::getCseKmsRegion));
    }

    Optional<GoogleCloudStorage> storageForGcs = getStorageFor(context, BackupStorage::getGcs);
    if (storageForGcs.isPresent()) {
      data.put("WALG_GS_PREFIX", getFromGcs(storageForGcs, GoogleCloudStorage::getPrefix)
          + "/" + namespace + "/" + name);
    }

    Optional<AzureBlobStorage> storageForAzureBlob = getStorageFor(
        context, BackupStorage::getAzureblob);
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

  }

  protected static String getHostFromUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  protected static int getPortFromUrl(String url) throws MalformedURLException {
    URL parsedUrl = new URL(url);
    int port = parsedUrl.getPort();
    if (port == -1) {
      if (parsedUrl.getProtocol().equals("https")) {
        return 443;
      } else {
        return 80;
      }
    } else {
      return port;
    }
  }

  private static <T> void putIfPresent(String env, T p, Map<String, String> data) {
    Optional.ofNullable(p).ifPresent(value -> data.put(env, value.toString()));
  }

  private static <T> boolean hasFromConfig(StackGresClusterContext context,
                                           Function<StackGresBackupConfigSpec, T> getter) {
    return context.getBackupConfig()
        .map(StackGresBackupConfig::getSpec)
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .isPresent();
  }

  private static <T> String getFromConfig(StackGresClusterContext context,
                                          Function<StackGresBackupConfigSpec, T> getter) {
    return context.getBackupConfig()
        .map(StackGresBackupConfig::getSpec)
        .map(getter)
        .map(PatroniConfigMap::convertEnvValue)
        .orElse("");
  }

  private static <T> Optional<T> getStorageFor(StackGresClusterContext context,
                                               Function<BackupStorage, T> getter) {
    return context.getBackupConfig()
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
