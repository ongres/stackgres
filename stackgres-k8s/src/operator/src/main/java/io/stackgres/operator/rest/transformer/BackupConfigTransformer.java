/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.rest.dto.SecretKeySelector;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigSpec;
import io.stackgres.operator.rest.dto.storages.AwsCredentials;
import io.stackgres.operator.rest.dto.storages.AwsS3CompatibleStorage;
import io.stackgres.operator.rest.dto.storages.AwsS3Storage;
import io.stackgres.operator.rest.dto.storages.AzureBlobStorage;
import io.stackgres.operator.rest.dto.storages.AzureBlobStorageCredentials;
import io.stackgres.operator.rest.dto.storages.BackupStorage;
import io.stackgres.operator.rest.dto.storages.GoogleCloudCredentials;
import io.stackgres.operator.rest.dto.storages.GoogleCloudStorage;

@ApplicationScoped
public class BackupConfigTransformer
    extends AbstractResourceTransformer<BackupConfigDto, StackGresBackupConfig> {

  @Override
  public StackGresBackupConfig toCustomResource(BackupConfigDto source) {
    StackGresBackupConfig transformation = new StackGresBackupConfig();
    transformation.setMetadata(getCustomResourceMetadata(source));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public BackupConfigDto toResource(StackGresBackupConfig source) {
    BackupConfigDto transformation = new BackupConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  public StackGresBackupConfigSpec getCustomResourceSpec(BackupConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresBackupConfigSpec transformation = new StackGresBackupConfigSpec();
    transformation.setCompressionMethod(source.getCompressionMethod());
    transformation.setDiskRateLimit(source.getDiskRateLimit());
    transformation.setFullSchedule(source.getFullSchedule());
    transformation.setFullWindow(source.getFullWindow());
    transformation.setNetworkRateLimit(source.getNetworkRateLimit());
    transformation.setRetention(source.getRetention());
    transformation.setStorage(
        getCustomResourceStorage(source.getStorage()));
    transformation.setTarSizeThreshold(source.getTarSizeThreshold());
    transformation.setUploadDiskConcurrency(source.getUploadDiskConcurrency());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.BackupStorage
      getCustomResourceStorage(BackupStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.BackupStorage transformation =
        new io.stackgres.operator.customresource.storages.BackupStorage();
    transformation.setAzureblob(
        getCustomResourceAzureblobStorage(source.getAzureblob()));
    transformation.setGcs(
        getCustomResourceGcsStorage(source.getGcs()));
    transformation.setS3(
        getCustomResourceS3Storage(source.getS3()));
    transformation.setS3Compatible(
        getCustomResourceS3CompatibleStorage(source.getS3Compatible()));
    transformation.setType(source.getType());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AzureBlobStorage
      getCustomResourceAzureblobStorage(AzureBlobStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AzureBlobStorage transformation =
        new io.stackgres.operator.customresource.storages.AzureBlobStorage();
    transformation.setCredentials(
        getCustomResourceAzureblobStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials
      getCustomResourceAzureblobStorageCredentials(
      AzureBlobStorageCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials
        transformation =
        new io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials();
    setSecretKeySelector(transformation::setAccessKey,
        source.getAccessKeySelector());
    setSecretKeySelector(transformation::setAccount,
        source.getAccountSelector());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.GoogleCloudStorage
      getCustomResourceGcsStorage(GoogleCloudStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.GoogleCloudStorage
        transformation =
        new io.stackgres.operator.customresource.storages.GoogleCloudStorage();
    transformation.setCredentials(
        getCustomResourceGcsStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.GoogleCloudCredentials
      getCustomResourceGcsStorageCredentials(GoogleCloudCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.GoogleCloudCredentials
        transformation =
        new io.stackgres.operator.customresource.storages.GoogleCloudCredentials();
    setSecretKeySelector(transformation::setServiceAccountJsonKey,
        source.getServiceAccountJsonKeySelector());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AwsS3Storage
      getCustomResourceS3Storage(AwsS3Storage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AwsS3Storage transformation =
        new io.stackgres.operator.customresource.storages.AwsS3Storage();
    transformation.setCredentials(
        getCustomResourceAwsCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AwsS3CompatibleStorage
      getCustomResourceS3CompatibleStorage(AwsS3CompatibleStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AwsS3CompatibleStorage transformation =
        new io.stackgres.operator.customresource.storages.AwsS3CompatibleStorage();
    transformation.setCredentials(
        getCustomResourceAwsCredentials(source.getCredentials()));
    transformation.setEndpoint(source.getEndpoint());
    transformation.setForcePathStyle(source.isForcePathStyle());
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AwsCredentials
      getCustomResourceAwsCredentials(AwsCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AwsCredentials
        transformation =
        new io.stackgres.operator.customresource.storages.AwsCredentials();
    setSecretKeySelector(transformation::setAccessKey,
        source.getAccessKeySelector());
    setSecretKeySelector(transformation::setSecretKey,
        source.getSecretKeySelector());
    return transformation;
  }

  public BackupConfigSpec getResourceSpec(StackGresBackupConfigSpec source) {
    if (source == null) {
      return null;
    }
    BackupConfigSpec transformation = new BackupConfigSpec();
    transformation.setCompressionMethod(source.getCompressionMethod());
    transformation.setDiskRateLimit(source.getDiskRateLimit());
    transformation.setFullSchedule(source.getFullSchedule());
    transformation.setFullWindow(source.getFullWindow());
    transformation.setNetworkRateLimit(source.getNetworkRateLimit());
    transformation.setRetention(source.getRetention());
    transformation.setStorage(
        getResourceStorage(source.getStorage()));
    transformation.setTarSizeThreshold(source.getTarSizeThreshold());
    transformation.setUploadDiskConcurrency(source.getUploadDiskConcurrency());
    return transformation;
  }

  private BackupStorage getResourceStorage(
      io.stackgres.operator.customresource.storages.BackupStorage source) {
    if (source == null) {
      return null;
    }
    BackupStorage transformation = new BackupStorage();
    transformation.setAzureblob(
        getResourceAzureblobStorage(source.getAzureblob()));
    transformation.setGcs(
        getResourceGcsStorage(source.getGcs()));
    transformation.setS3(
        getResourceS3Storage(source.getS3()));
    transformation.setS3Compatible(
        getResourceS3CompatibleStorage(source.getS3Compatible()));
    transformation.setType(source.getType());
    return transformation;
  }

  private AzureBlobStorage getResourceAzureblobStorage(
      io.stackgres.operator.customresource.storages.AzureBlobStorage source) {
    if (source == null) {
      return null;
    }
    AzureBlobStorage transformation = new AzureBlobStorage();
    transformation.setCredentials(
        getResourceAzureblobStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private AzureBlobStorageCredentials getResourceAzureblobStorageCredentials(
      io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials source) {
    if (source == null) {
      return null;
    }
    AzureBlobStorageCredentials transformation =
        new AzureBlobStorageCredentials();
    transformation.setAccessKeySelector(
        SecretKeySelector.create(
            source.getAccessKey().getName(),
            source.getAccessKey().getKey()));
    transformation.setAccountSelector(
        SecretKeySelector.create(
            source.getAccount().getName(),
            source.getAccount().getKey()));
    return transformation;
  }

  private GoogleCloudStorage getResourceGcsStorage(
      io.stackgres.operator.customresource.storages.GoogleCloudStorage source) {
    if (source == null) {
      return null;
    }
    GoogleCloudStorage transformation = new GoogleCloudStorage();
    transformation.setCredentials(
        getResourceGcsStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private GoogleCloudCredentials getResourceGcsStorageCredentials(
      io.stackgres.operator.customresource.storages.GoogleCloudCredentials source) {
    if (source == null) {
      return null;
    }
    GoogleCloudCredentials transformation =
        new GoogleCloudCredentials();
    transformation.setServiceAccountJsonKeySelector(
        SecretKeySelector.create(
            source.getServiceAccountJsonKey().getName(),
            source.getServiceAccountJsonKey().getKey()));
    return transformation;
  }

  private AwsS3Storage getResourceS3Storage(
      io.stackgres.operator.customresource.storages.AwsS3Storage source) {
    if (source == null) {
      return null;
    }
    AwsS3Storage transformation = new AwsS3Storage();
    transformation.setCredentials(
        getResourceAwsCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private AwsS3CompatibleStorage getResourceS3CompatibleStorage(
      io.stackgres.operator.customresource.storages.AwsS3CompatibleStorage source) {
    if (source == null) {
      return null;
    }
    AwsS3CompatibleStorage transformation = new AwsS3CompatibleStorage();
    transformation.setCredentials(
        getResourceAwsCredentials(source.getCredentials()));
    transformation.setEndpoint(source.getEndpoint());
    transformation.setForcePathStyle(source.isForcePathStyle());
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private AwsCredentials getResourceAwsCredentials(
      io.stackgres.operator.customresource.storages.AwsCredentials source) {
    if (source == null) {
      return null;
    }
    AwsCredentials transformation = new AwsCredentials();
    transformation.setAccessKeySelector(
        SecretKeySelector.create(
            source.getAccessKey().getName(),
            source.getAccessKey().getKey()));
    transformation.setSecretKeySelector(
        SecretKeySelector.create(
            source.getSecretKey().getName(),
            source.getSecretKey().getKey()));
    return transformation;
  }

  private void setSecretKeySelector(
      Consumer<io.fabric8.kubernetes.api.model.SecretKeySelector> setter,
      SecretKeySelector secretKeySelector) {
    if (secretKeySelector != null) {
      setter.accept(new io.fabric8.kubernetes.api.model.SecretKeySelector(
          secretKeySelector.getKey(),
          secretKeySelector.getName(),
          false));
    }
  }

}
