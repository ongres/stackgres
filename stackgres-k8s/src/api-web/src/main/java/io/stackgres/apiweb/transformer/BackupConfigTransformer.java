/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.apiweb.distributedlogs.dto.SecretKeySelector;
import io.stackgres.apiweb.distributedlogs.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.distributedlogs.dto.backupconfig.BackupConfigSpec;
import io.stackgres.apiweb.distributedlogs.dto.backupconfig.BackupConfigStatus;
import io.stackgres.apiweb.distributedlogs.dto.backupconfig.BaseBackupConfig;
import io.stackgres.apiweb.distributedlogs.dto.backupconfig.BaseBackupPerformance;
import io.stackgres.apiweb.distributedlogs.dto.storages.AwsCredentials;
import io.stackgres.apiweb.distributedlogs.dto.storages.AwsS3CompatibleStorage;
import io.stackgres.apiweb.distributedlogs.dto.storages.AwsS3Storage;
import io.stackgres.apiweb.distributedlogs.dto.storages.AzureBlobSecretKeySelector;
import io.stackgres.apiweb.distributedlogs.dto.storages.AzureBlobStorage;
import io.stackgres.apiweb.distributedlogs.dto.storages.AzureBlobStorageCredentials;
import io.stackgres.apiweb.distributedlogs.dto.storages.BackupStorage;
import io.stackgres.apiweb.distributedlogs.dto.storages.GoogleCloudCredentials;
import io.stackgres.apiweb.distributedlogs.dto.storages.GoogleCloudSecretKeySelector;
import io.stackgres.apiweb.distributedlogs.dto.storages.GoogleCloudStorage;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;

@ApplicationScoped
public class BackupConfigTransformer
    extends AbstractDependencyResourceTransformer<BackupConfigDto, StackGresBackupConfig> {

  @Override
  public StackGresBackupConfig toCustomResource(BackupConfigDto source,
      StackGresBackupConfig original) {
    StackGresBackupConfig transformation = Optional.ofNullable(original)
        .orElseGet(StackGresBackupConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public BackupConfigDto toResource(StackGresBackupConfig source, List<String> clusters) {
    BackupConfigDto transformation = new BackupConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters));
    return transformation;
  }

  public StackGresBackupConfigSpec getCustomResourceSpec(BackupConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresBackupConfigSpec transformation = new StackGresBackupConfigSpec();
    Optional.ofNullable(source.getBaseBackups())
        .ifPresent(sourceBaseBackup -> {
          final StackGresBaseBackupConfig baseBackup = new StackGresBaseBackupConfig();
          transformation.setBaseBackups(baseBackup);
          baseBackup.setCompression(source.getBaseBackups().getCompressionMethod());
          baseBackup.setCronSchedule(sourceBaseBackup.getCronSchedule());
          baseBackup.setRetention(sourceBaseBackup.getRetention());
        });

    Optional.ofNullable(source.getBaseBackups())
        .map(BaseBackupConfig::getPerformance)
        .ifPresent(sourcePerformance -> {
          final StackGresBaseBackupPerformance performance = new StackGresBaseBackupPerformance();
          transformation.getBaseBackups().setPerformance(performance);
          performance.setMaxDiskBandwitdh(sourcePerformance.getMaxDiskBandwitdh());
          performance.setMaxNetworkBandwitdh(sourcePerformance.getMaxNetworkBandwitdh());
          performance.setUploadDiskConcurrency(sourcePerformance.getUploadDiskConcurrency());
        });

    transformation.setStorage(getCustomResourceStorage(source.getStorage()));
    return transformation;
  }

  private io.stackgres.common.crd.storages.BackupStorage
      getCustomResourceStorage(BackupStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.BackupStorage transformation =
        new io.stackgres.common.crd.storages.BackupStorage();
    transformation.setAzureBlob(
        getCustomResourceAzureblobStorage(source.getAzureBlob()));
    transformation.setGcs(
        getCustomResourceGcsStorage(source.getGcs()));
    transformation.setS3(
        getCustomResourceS3Storage(source.getS3()));
    transformation.setS3Compatible(
        getCustomResourceS3CompatibleStorage(source.getS3Compatible()));
    transformation.setType(source.getType());
    return transformation;
  }

  private io.stackgres.common.crd.storages.AzureBlobStorage
      getCustomResourceAzureblobStorage(AzureBlobStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.AzureBlobStorage transformation =
        new io.stackgres.common.crd.storages.AzureBlobStorage();
    transformation.setAzureCredentials(
        getCustomResourceAzureblobStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private io.stackgres.common.crd.storages.AzureBlobStorageCredentials
      getCustomResourceAzureblobStorageCredentials(AzureBlobStorageCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.AzureBlobStorageCredentials
        transformation =
        new io.stackgres.common.crd.storages.AzureBlobStorageCredentials();
    final io.stackgres.common.crd.storages.AzureBlobSecretKeySelector
        secretKeySelectors =
        new io.stackgres.common.crd.storages.AzureBlobSecretKeySelector();
    transformation.setSecretKeySelectors(secretKeySelectors);
    setSecretKeySelector(secretKeySelectors::setAccessKey,
        source.getSecretKeySelectors().getAccessKey());
    setSecretKeySelector(secretKeySelectors::setAccount,
        source.getSecretKeySelectors().getAccount());
    return transformation;
  }

  private io.stackgres.common.crd.storages.GoogleCloudStorage
      getCustomResourceGcsStorage(GoogleCloudStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.GoogleCloudStorage
        transformation =
        new io.stackgres.common.crd.storages.GoogleCloudStorage();
    transformation.setCredentials(
        getCustomResourceGcsStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private io.stackgres.common.crd.storages.GoogleCloudCredentials
      getCustomResourceGcsStorageCredentials(GoogleCloudCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.GoogleCloudCredentials
        transformation =
        new io.stackgres.common.crd.storages.GoogleCloudCredentials();
    if (source.getSecretKeySelectors().getServiceAccountJsonKey() != null) {
      final io.stackgres.common.crd.storages.GoogleCloudSecretKeySelector
          secretKeySelectors =
          new io.stackgres.common.crd.storages.GoogleCloudSecretKeySelector();
      transformation.setSecretKeySelectors(secretKeySelectors);
      setSecretKeySelector(secretKeySelectors::setServiceAccountJsonKey,
          source.getSecretKeySelectors().getServiceAccountJsonKey());
    }
    return transformation;
  }

  private io.stackgres.common.crd.storages.AwsS3Storage
      getCustomResourceS3Storage(AwsS3Storage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.AwsS3Storage transformation =
        new io.stackgres.common.crd.storages.AwsS3Storage();
    transformation.setAwsCredentials(
        getCustomResourceAwsCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private io.stackgres.common.crd.storages.AwsS3CompatibleStorage
      getCustomResourceS3CompatibleStorage(AwsS3CompatibleStorage source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.AwsS3CompatibleStorage transformation =
        new io.stackgres.common.crd.storages.AwsS3CompatibleStorage();
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

  private io.stackgres.common.crd.storages.AwsCredentials
      getCustomResourceAwsCredentials(AwsCredentials source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.storages.AwsCredentials
        transformation =
        new io.stackgres.common.crd.storages.AwsCredentials();
    final AwsSecretKeySelector secretKeySelectors = new AwsSecretKeySelector();
    transformation.setSecretKeySelectors(secretKeySelectors);
    setSecretKeySelector(secretKeySelectors::setAccessKeyId,
        source.getSecretKeySelectors().getAccessKeyId());
    setSecretKeySelector(secretKeySelectors::setSecretAccessKey,
        source.getSecretKeySelectors().getSecretAccessKey());
    return transformation;
  }

  public BackupConfigSpec getResourceSpec(StackGresBackupConfigSpec source) {
    if (source == null) {
      return null;
    }
    BackupConfigSpec transformation = new BackupConfigSpec();
    Optional.ofNullable(source.getBaseBackups())
        .ifPresent(sourceBaseBackup -> {
          final BaseBackupConfig baseBackup = new BaseBackupConfig();
          baseBackup.setCompressionMethod(sourceBaseBackup.getCompression());
          baseBackup.setCronSchedule(sourceBaseBackup.getCronSchedule());
          baseBackup.setRetention(sourceBaseBackup.getRetention());
          transformation.setBaseBackup(baseBackup);
        });

    Optional.ofNullable(source.getBaseBackups())
        .map(StackGresBaseBackupConfig::getPerformance)
        .ifPresent(sourcePerformance -> {
          final BaseBackupPerformance performance = new BaseBackupPerformance();
          performance.setMaxDiskBandwitdh(sourcePerformance.getMaxDiskBandwitdh());
          performance.setMaxNetworkBandwitdh(sourcePerformance.getMaxNetworkBandwitdh());
          performance.setUploadDiskConcurrency(sourcePerformance.getUploadDiskConcurrency());

          transformation.getBaseBackups().setPerformance(performance);
        });

    transformation.setStorage(getResourceStorage(source.getStorage()));
    return transformation;
  }

  public BackupConfigStatus getResourceStatus(List<String> clusters) {
    BackupConfigStatus transformation = new BackupConfigStatus();
    transformation.setClusters(clusters);
    return transformation;
  }

  private BackupStorage getResourceStorage(
      io.stackgres.common.crd.storages.BackupStorage source) {
    if (source == null) {
      return null;
    }
    BackupStorage transformation = new BackupStorage();
    transformation.setAzureBlob(
        getResourceAzureblobStorage(source.getAzureBlob()));
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
      io.stackgres.common.crd.storages.AzureBlobStorage source) {
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
      io.stackgres.common.crd.storages.AzureBlobStorageCredentials source) {
    if (source == null) {
      return null;
    }
    AzureBlobStorageCredentials transformation =
        new AzureBlobStorageCredentials();
    if (source.getSecretKeySelectors() != null) {
      final AzureBlobSecretKeySelector secretKeySelectors = new AzureBlobSecretKeySelector();
      transformation.setSecretKeySelectors(secretKeySelectors);
      secretKeySelectors.setAccessKey(
          SecretKeySelector.create(
              source.getSecretKeySelectors().getAccessKey().getName(),
              source.getSecretKeySelectors().getAccessKey().getKey()));
      secretKeySelectors.setAccount(
          SecretKeySelector.create(
              source.getSecretKeySelectors().getAccount().getName(),
              source.getSecretKeySelectors().getAccount().getKey()));
    }
    return transformation;
  }

  private GoogleCloudStorage getResourceGcsStorage(
      io.stackgres.common.crd.storages.GoogleCloudStorage source) {
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
      io.stackgres.common.crd.storages.GoogleCloudCredentials source) {
    if (source == null) {
      return null;
    }
    GoogleCloudCredentials transformation = new GoogleCloudCredentials();
    if (source.getSecretKeySelectors() != null) {
      final GoogleCloudSecretKeySelector secretKeySelectors = new GoogleCloudSecretKeySelector();
      transformation.setSecretKeySelectors(secretKeySelectors);
      secretKeySelectors.setServiceAccountJsonKey(
          SecretKeySelector.create(
              source.getSecretKeySelectors().getServiceAccountJsonKey().getName(),
              source.getSecretKeySelectors().getServiceAccountJsonKey().getKey()));
    }
    return transformation;
  }

  private AwsS3Storage getResourceS3Storage(
      io.stackgres.common.crd.storages.AwsS3Storage source) {
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
      io.stackgres.common.crd.storages.AwsS3CompatibleStorage source) {
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
      io.stackgres.common.crd.storages.AwsCredentials source) {
    if (source == null) {
      return null;
    }
    AwsCredentials transformation = new AwsCredentials();
    if (source.getSecretKeySelectors() != null) {
      final AwsSecretKeySelector secretKeySelectors = source.getSecretKeySelectors();
      transformation.getSecretKeySelectors().setAccessKeyId(
          SecretKeySelector.create(
              secretKeySelectors.getAccessKeyId().getName(),
              secretKeySelectors.getAccessKeyId().getKey()));
      transformation.getSecretKeySelectors().setSecretAccessKey(
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
