/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.Optional;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.operator.customresource.storages.AwsSecretKeySelector;
import io.stackgres.operator.customresource.storages.AzureBlobSecretKeySelector;
import io.stackgres.operator.customresource.storages.GoogleCloudSecretKeySelector;
import io.stackgres.operator.rest.dto.SecretKeySelector;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigSpec;
import io.stackgres.operator.rest.dto.backupconfig.BaseBackupConfig;
import io.stackgres.operator.rest.dto.backupconfig.BaseBackupPerformance;
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
    StackGresBackupConfigSpec transformation = new StackGresBackupConfigSpec();
    Optional.ofNullable(source.getBaseBackup())
        .ifPresent(sourceBaseBackup -> {
          final StackGresBaseBackupConfig baseBackup = new StackGresBaseBackupConfig();
          transformation.setBaseBackups(baseBackup);
          baseBackup.setCompression(source.getBaseBackup().getCompressionMethod());
          baseBackup.setCronSchedule(sourceBaseBackup.getFullSchedule());
          baseBackup.setRetention(sourceBaseBackup.getRetention());
        });

    Optional.ofNullable(source.getBaseBackup())
        .map(BaseBackupConfig::getPerformance)
        .ifPresent(sourcePerformance -> {
          final StackGresBaseBackupPerformance performance = new StackGresBaseBackupPerformance();
          transformation.getBaseBackups().setPerformance(performance);
          performance.setMaxDiskBandwitdh(sourcePerformance.getDiskRateLimit());
          performance.setMaxNetworkBandwitdh(sourcePerformance.getNetworkRateLimit());
          performance.setUploadDiskConcurrency(sourcePerformance.getUploadDiskConcurrency());
        });

    transformation.setStorage(getCustomResourceStorage(source.getStorage()));
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
    transformation.setAzureCredentials(
        getCustomResourceAzureblobStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials
      getCustomResourceAzureblobStorageCredentials(AzureBlobStorageCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials
        transformation =
        new io.stackgres.operator.customresource.storages.AzureBlobStorageCredentials();
    final AzureBlobSecretKeySelector secretKeySelectors = new AzureBlobSecretKeySelector();
    transformation.setSecretKeySelectors(secretKeySelectors);
    setSecretKeySelector(secretKeySelectors::setAccessKey,
        source.getAccessKeySelector());
    setSecretKeySelector(secretKeySelectors::setAccount,
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
    if (source.getServiceAccountJsonKeySelector() != null) {
      final GoogleCloudSecretKeySelector secretKeySelectors = new GoogleCloudSecretKeySelector();
      transformation.setSecretKeySelectors(secretKeySelectors);
      setSecretKeySelector(secretKeySelectors::setServiceAccountJsonKey,
          source.getServiceAccountJsonKeySelector());
    }
    return transformation;
  }

  private io.stackgres.operator.customresource.storages.AwsS3Storage
      getCustomResourceS3Storage(AwsS3Storage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.storages.AwsS3Storage transformation =
        new io.stackgres.operator.customresource.storages.AwsS3Storage();
    transformation.setAwsCredentials(
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
    transformation.setAwsCredentials(
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
    final AwsSecretKeySelector secretKeySelectors = new AwsSecretKeySelector();
    transformation.setSecretKeySelectors(secretKeySelectors);
    setSecretKeySelector(secretKeySelectors::setAccessKeyId,
        source.getSecretKeySelectors().getAccessKeySelector());
    setSecretKeySelector(secretKeySelectors::setSecretAccessKey,
        source.getSecretKeySelectors().getSecretKeySelector());
    return transformation;
  }

  public BackupConfigSpec getResourceSpec(StackGresBackupConfigSpec source) {
    BackupConfigSpec transformation = new BackupConfigSpec();
    Optional.ofNullable(source.getBaseBackups())
        .ifPresent(sourceBaseBackup -> {
          final BaseBackupConfig baseBackup = new BaseBackupConfig();
          baseBackup.setCompressionMethod(sourceBaseBackup.getCompression());
          baseBackup.setFullSchedule(sourceBaseBackup.getCronSchedule());
          baseBackup.setRetention(sourceBaseBackup.getRetention());
          transformation.setBaseBackup(baseBackup);
        });

    Optional.ofNullable(source.getBaseBackups())
        .map(StackGresBaseBackupConfig::getPerformance)
        .ifPresent(sourcePerformance -> {
          final BaseBackupPerformance performance = new BaseBackupPerformance();
          performance.setDiskRateLimit(sourcePerformance.getMaxDiskBandwitdh());
          performance.setNetworkRateLimit(sourcePerformance.getMaxNetworkBandwitdh());
          performance.setUploadDiskConcurrency(sourcePerformance.getUploadDiskConcurrency());

          transformation.getBaseBackup().setPerformance(performance);
        });

    transformation.setStorage(getResourceStorage(source.getStorage()));
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
        getResourceAzureblobStorageCredentials(source.getAzureCredentials()));
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
            source.getSecretKeySelectors().getAccessKey().getName(),
            source.getSecretKeySelectors().getAccessKey().getKey()));
    transformation.setAccountSelector(
        SecretKeySelector.create(
            source.getSecretKeySelectors().getAccount().getName(),
            source.getSecretKeySelectors().getAccount().getKey()));
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
            source.getSecretKeySelectors().getServiceAccountJsonKey().getName(),
            source.getSecretKeySelectors().getServiceAccountJsonKey().getKey()));
    return transformation;
  }

  private AwsS3Storage getResourceS3Storage(
      io.stackgres.operator.customresource.storages.AwsS3Storage source) {
    if (source == null) {
      return null;
    }
    AwsS3Storage transformation = new AwsS3Storage();
    transformation.setCredentials(
        getResourceAwsCredentials(source.getAwsCredentials()));
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
        getResourceAwsCredentials(source.getAwsCredentials()));
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
    if (source.getSecretKeySelectors() != null) {
      final AwsSecretKeySelector secretKeySelectors = source.getSecretKeySelectors();
      transformation.getSecretKeySelectors().setAccessKeySelector(
          SecretKeySelector.create(
              secretKeySelectors.getAccessKeyId().getName(),
              secretKeySelectors.getAccessKeyId().getKey()));
      transformation.getSecretKeySelectors().setSecretKeySelector(
          SecretKeySelector.create(
              secretKeySelectors.getSecretAccessKey().getName(),
              secretKeySelectors.getSecretAccessKey().getKey()));
    }
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
