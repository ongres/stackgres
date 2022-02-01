/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.apiweb.dto.storages.AwsCredentialsDto;
import io.stackgres.apiweb.dto.storages.AwsS3CompatibleStorageDto;
import io.stackgres.apiweb.dto.storages.AwsS3StorageDto;
import io.stackgres.apiweb.dto.storages.AzureBlobSecretKeySelectorDto;
import io.stackgres.apiweb.dto.storages.AzureBlobStorageCredentialsDto;
import io.stackgres.apiweb.dto.storages.AzureBlobStorageDto;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.apiweb.dto.storages.GoogleCloudCredentialsDto;
import io.stackgres.apiweb.dto.storages.GoogleCloudSecretKeySelectorDto;
import io.stackgres.apiweb.dto.storages.GoogleCloudStorageDto;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.AzureBlobStorageCredentials;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudSecretKeySelector;
import io.stackgres.common.crd.storages.GoogleCloudStorage;

@ApplicationScoped
public class BackupStorageTransformer implements Transformer<BackupStorageDto, BackupStorage> {

  @Override
  public BackupStorageDto toSource(BackupStorage target) {
    if (target == null) {
      return null;
    }
    BackupStorageDto transformation = new BackupStorageDto();
    transformation.setAzureBlob(
        getResourceAzureblobStorage(target.getAzureBlob()));
    transformation.setGcs(
        getResourceGcsStorage(target.getGcs()));
    transformation.setS3(
        getResourceS3Storage(target.getS3()));
    transformation.setS3Compatible(
        getResourceS3CompatibleStorage(target.getS3Compatible()));
    transformation.setType(target.getType());
    return transformation;
  }

  @Override
  public BackupStorage toTarget(BackupStorageDto source) {
    if (source == null) {
      return null;
    }
    BackupStorage transformation =
        new BackupStorage();
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

  private AzureBlobStorage getCustomResourceAzureblobStorage(AzureBlobStorageDto source) {
    if (source == null) {
      return null;
    }
    AzureBlobStorage transformation =
        new AzureBlobStorage();
    transformation.setAzureCredentials(
        getCustomResourceAzureblobStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private AzureBlobStorageCredentials getCustomResourceAzureblobStorageCredentials(
      AzureBlobStorageCredentialsDto source
  ) {
    if (source == null) {
      return null;
    }
    AzureBlobStorageCredentials transformation = new AzureBlobStorageCredentials();
    final AzureBlobSecretKeySelector secretKeySelectors = new AzureBlobSecretKeySelector();
    transformation.setSecretKeySelectors(secretKeySelectors);
    secretKeySelectors.setAccessKey(
        source.getSecretKeySelectors().getAccessKey());
    secretKeySelectors.setAccount(
        source.getSecretKeySelectors().getAccount());
    return transformation;
  }

  private GoogleCloudStorage getCustomResourceGcsStorage(GoogleCloudStorageDto source) {
    if (source == null) {
      return null;
    }
    GoogleCloudStorage
        transformation =
        new GoogleCloudStorage();
    transformation.setCredentials(
        getCustomResourceGcsStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private GoogleCloudCredentials getCustomResourceGcsStorageCredentials(
      GoogleCloudCredentialsDto source
  ) {
    if (source == null) {
      return null;
    }
    GoogleCloudCredentials transformation = new GoogleCloudCredentials();
    transformation.setFetchCredentialsFromMetadataService(
        source.isFetchCredentialsFromMetadataService());
    final GoogleCloudSecretKeySelectorDto sourceSecretKeySelectors = source.getSecretKeySelectors();

    Optional.ofNullable(source.getSecretKeySelectors())
        .map(GoogleCloudSecretKeySelectorDto::getServiceAccountJsonKey)
        .ifPresent(sourceServiceAccountJsonKey -> {
          final GoogleCloudSecretKeySelector targetSecretKeySelectors =
              new GoogleCloudSecretKeySelector();
          transformation.setSecretKeySelectors(targetSecretKeySelectors);
          targetSecretKeySelectors.setServiceAccountJsonKey(
              sourceSecretKeySelectors.getServiceAccountJsonKey());

        });
    return transformation;
  }

  private AwsS3Storage getCustomResourceS3Storage(
      AwsS3StorageDto source
  ) {
    if (source == null) {
      return null;
    }
    AwsS3Storage transformation =
        new AwsS3Storage();
    transformation.setAwsCredentials(
        getCustomResourceAwsCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private AwsS3CompatibleStorage getCustomResourceS3CompatibleStorage(
      AwsS3CompatibleStorageDto source
  ) {
    if (source == null) {
      return null;
    }
    AwsS3CompatibleStorage transformation =
        new AwsS3CompatibleStorage();
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

  private AwsCredentials getCustomResourceAwsCredentials(AwsCredentialsDto source) {
    if (source == null) {
      return null;
    }
    AwsCredentials transformation = new AwsCredentials();
    final AwsSecretKeySelector secretKeySelectors = new AwsSecretKeySelector();
    transformation.setSecretKeySelectors(secretKeySelectors);
    secretKeySelectors.setAccessKeyId(
        source.getSecretKeySelectors().getAccessKeyId());
    secretKeySelectors.setSecretAccessKey(
        source.getSecretKeySelectors().getSecretAccessKey());
    return transformation;
  }

  private AzureBlobStorageDto getResourceAzureblobStorage(AzureBlobStorage source) {
    if (source == null) {
      return null;
    }
    AzureBlobStorageDto transformation = new AzureBlobStorageDto();
    transformation.setCredentials(
        getResourceAzureblobStorageCredentials(source.getAzureCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private AzureBlobStorageCredentialsDto getResourceAzureblobStorageCredentials(
      AzureBlobStorageCredentials source
  ) {
    if (source == null) {
      return null;
    }
    AzureBlobStorageCredentialsDto transformation =
        new AzureBlobStorageCredentialsDto();
    if (source.getSecretKeySelectors() != null) {
      final AzureBlobSecretKeySelectorDto secretKeySelectors = new AzureBlobSecretKeySelectorDto();
      transformation.setSecretKeySelectors(secretKeySelectors);
      secretKeySelectors.setAccessKey(
          source.getSecretKeySelectors().getAccessKey());
      secretKeySelectors.setAccount(
          source.getSecretKeySelectors().getAccount());
    }
    return transformation;
  }

  private GoogleCloudStorageDto getResourceGcsStorage(GoogleCloudStorage source) {
    if (source == null) {
      return null;
    }
    GoogleCloudStorageDto transformation = new GoogleCloudStorageDto();
    transformation.setCredentials(
        getResourceGcsStorageCredentials(source.getCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    return transformation;
  }

  private GoogleCloudCredentialsDto getResourceGcsStorageCredentials(
      GoogleCloudCredentials source
  ) {
    if (source == null) {
      return null;
    }
    GoogleCloudCredentialsDto transformation = new GoogleCloudCredentialsDto();
    transformation.setFetchCredentialsFromMetadataService(
        source.isFetchCredentialsFromMetadataService());
    if (source.getSecretKeySelectors() != null) {
      final GoogleCloudSecretKeySelectorDto secretKeySelectors =
          new GoogleCloudSecretKeySelectorDto();
      transformation.setSecretKeySelectors(secretKeySelectors);
      secretKeySelectors.setServiceAccountJsonKey(
          source.getSecretKeySelectors().getServiceAccountJsonKey());
    }
    return transformation;
  }

  private AwsS3StorageDto getResourceS3Storage(AwsS3Storage source) {
    if (source == null) {
      return null;
    }
    AwsS3StorageDto transformation = new AwsS3StorageDto();
    transformation.setCredentials(
        getResourceAwsCredentials(source.getAwsCredentials()));
    transformation.setBucket(source.getBucket());
    transformation.setPath(source.getPath());
    transformation.setRegion(source.getRegion());
    transformation.setStorageClass(source.getStorageClass());
    return transformation;
  }

  private AwsS3CompatibleStorageDto getResourceS3CompatibleStorage(AwsS3CompatibleStorage source) {
    if (source == null) {
      return null;
    }
    AwsS3CompatibleStorageDto transformation = new AwsS3CompatibleStorageDto();
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

  private AwsCredentialsDto getResourceAwsCredentials(AwsCredentials source) {
    if (source == null) {
      return null;
    }
    AwsCredentialsDto transformation = new AwsCredentialsDto();
    if (source.getSecretKeySelectors() != null) {
      final AwsSecretKeySelector secretKeySelectors = source.getSecretKeySelectors();
      transformation.getSecretKeySelectors().setAccessKeyId(
          secretKeySelectors.getAccessKeyId());
      transformation.getSecretKeySelectors().setSecretAccessKey(
          secretKeySelectors.getSecretAccessKey());
    }
    return transformation;
  }
}
