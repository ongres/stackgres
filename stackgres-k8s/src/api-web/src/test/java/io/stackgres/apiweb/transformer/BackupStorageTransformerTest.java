/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Random;

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
import io.stackgres.common.crd.SecretKeySelector;
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
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.Test;

class BackupStorageTransformerTest {

  Transformer<BackupStorageDto, BackupStorage> storageTransformer = new BackupStorageTransformer();

  public static TransformerTuple<BackupStorageDto, BackupStorage> createS3BackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "s3";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createS3Storage();
    crdBackupStorage.setS3(s3Storage.getSource());
    dtoBackupStorage.setS3(s3Storage.getTarget());

    return new TransformerTuple<>(dtoBackupStorage, crdBackupStorage);
  }

  public static TransformerTuple<BackupStorageDto,
      BackupStorage> createS3CompatibleBackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "s3Compatible";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createS3CompatibleStorage();
    crdBackupStorage.setS3Compatible(s3Storage.getSource());
    dtoBackupStorage.setS3Compatible(s3Storage.getTarget());

    return new TransformerTuple<>(dtoBackupStorage, crdBackupStorage);
  }

  public static TransformerTuple<BackupStorageDto, BackupStorage> createGcsBackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "gcs";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createGcsStorage();
    crdBackupStorage.setGcs(s3Storage.getSource());
    dtoBackupStorage.setGcs(s3Storage.getTarget());

    return new TransformerTuple<>(dtoBackupStorage, crdBackupStorage);
  }

  public static TransformerTuple<BackupStorageDto, BackupStorage> createAzureBackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "azureBlob";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createAzureBlobStorage();
    crdBackupStorage.setAzureBlob(s3Storage.getSource());
    dtoBackupStorage.setAzureBlob(s3Storage.getTarget());

    return new TransformerTuple<>(dtoBackupStorage, crdBackupStorage);
  }

  private static TransformerTuple<AzureBlobStorageDto, AzureBlobStorage> createAzureBlobStorage() {

    AzureBlobStorageDto azureBlobStorageDto = new AzureBlobStorageDto();
    AzureBlobStorage azureBlobStorage = new AzureBlobStorage();

    final String bucket = StringUtils.getRandomString(10);
    azureBlobStorageDto.setBucket(bucket);
    azureBlobStorage.setBucket(bucket);

    final String path = StringUtils.getRandomString(10);
    azureBlobStorageDto.setPath(path);
    azureBlobStorage.setPath(path);

    final var azureCredentials = createAzureCredentials();
    azureBlobStorageDto.setCredentials(azureCredentials.getTarget());
    azureBlobStorage.setAzureCredentials(azureCredentials.getSource());

    return new TransformerTuple<>(azureBlobStorageDto, azureBlobStorage);
  }

  private static TransformerTuple<AwsS3StorageDto, AwsS3Storage> createS3Storage() {
    AwsS3Storage crdS3Storage = new AwsS3Storage();
    AwsS3StorageDto dtoS3Storage = new AwsS3StorageDto();

    final String storageClass = StringUtils.getRandomString(10);
    crdS3Storage.setStorageClass(storageClass);
    dtoS3Storage.setStorageClass(storageClass);

    final String region = StringUtils.getRandomString(7);
    crdS3Storage.setRegion(region);
    dtoS3Storage.setRegion(region);

    final String bucket = StringUtils.getRandomString(11);
    crdS3Storage.setBucket(bucket);
    dtoS3Storage.setBucket(bucket);

    var credentialsTuple = generateAwsCredentials();
    crdS3Storage.setAwsCredentials(credentialsTuple.getSource());
    dtoS3Storage.setCredentials(credentialsTuple.getTarget());

    return new TransformerTuple<>(dtoS3Storage, crdS3Storage);
  }

  private static TransformerTuple<
      AwsS3CompatibleStorageDto, AwsS3CompatibleStorage> createS3CompatibleStorage() {
    AwsS3CompatibleStorageDto target = new AwsS3CompatibleStorageDto();
    AwsS3CompatibleStorage source = new AwsS3CompatibleStorage();

    final String storageClass = StringUtils.getRandomString(10);
    target.setStorageClass(storageClass);
    source.setStorageClass(storageClass);

    final String region = StringUtils.getRandomString(7);
    target.setRegion(region);
    source.setRegion(region);

    final String bucket = StringUtils.getRandomString(11);
    target.setBucket(bucket);
    source.setBucket(bucket);

    final String path = StringUtils.getRandomString(10);
    target.setPath(path);
    source.setPath(path);

    final String endpoint = StringUtils.getRandomString(10);
    target.setEndpoint(endpoint);
    source.setEndpoint(endpoint);

    boolean forcePathStyle = new Random().nextBoolean();
    target.setForcePathStyle(forcePathStyle);
    source.setForcePathStyle(forcePathStyle);

    final var credentialTuple = generateAwsCredentials();

    target.setCredentials(credentialTuple.getTarget());
    source.setAwsCredentials(credentialTuple.getSource());

    return new TransformerTuple<>(target, source);
  }

  private static TransformerTuple<GoogleCloudStorageDto, GoogleCloudStorage> createGcsStorage() {
    GoogleCloudStorageDto dtoS3Storage = new GoogleCloudStorageDto();
    GoogleCloudStorage crdS3Storage = new GoogleCloudStorage();

    final String path = StringUtils.getRandomString(8);
    dtoS3Storage.setPath(path);
    crdS3Storage.setPath(path);

    final String bucket = StringUtils.getRandomString(11);
    crdS3Storage.setBucket(bucket);
    dtoS3Storage.setBucket(bucket);

    var credentialsTuple = generateGcsCredentials();
    crdS3Storage.setCredentials(credentialsTuple.getSource());
    dtoS3Storage.setCredentials(credentialsTuple.getTarget());

    return new TransformerTuple<>(dtoS3Storage, crdS3Storage);
  }

  private static TransformerTuple<AwsCredentialsDto, AwsCredentials> generateAwsCredentials() {
    AwsCredentials crdCredentials = new AwsCredentials();
    AwsCredentialsDto dtoCredentials = new AwsCredentialsDto();

    crdCredentials.setSecretKeySelectors(new AwsSecretKeySelector());
    dtoCredentials.setSecretKeySelectors(
        new io.stackgres.apiweb.dto.storages.AwsSecretKeySelector()
    );

    var accessKeySelector = generateSecretKeySelector();
    crdCredentials.getSecretKeySelectors().setAccessKeyId(accessKeySelector);
    dtoCredentials.getSecretKeySelectors().setAccessKeyId(accessKeySelector);

    var secretKeySelector = generateSecretKeySelector();

    crdCredentials.getSecretKeySelectors().setSecretAccessKey(secretKeySelector);
    dtoCredentials.getSecretKeySelectors().setSecretAccessKey(secretKeySelector);
    return new TransformerTuple<>(dtoCredentials, crdCredentials);
  }

  private static TransformerTuple<
      GoogleCloudCredentialsDto, GoogleCloudCredentials> generateGcsCredentials() {
    GoogleCloudCredentials crdCredentials = new GoogleCloudCredentials();
    GoogleCloudCredentialsDto dtoCredentials = new GoogleCloudCredentialsDto();

    boolean fetchCredentialsFromMetadataService = new Random().nextBoolean();

    crdCredentials.setFetchCredentialsFromMetadataService(fetchCredentialsFromMetadataService);
    dtoCredentials.setFetchCredentialsFromMetadataService(fetchCredentialsFromMetadataService);

    var selector = createGoogleCloudSecretKeySelector();
    crdCredentials.setSecretKeySelectors(selector.getSource());
    dtoCredentials.setSecretKeySelectors(selector.getTarget());

    return new TransformerTuple<>(dtoCredentials, crdCredentials);
  }

  public static TransformerTuple<
      AzureBlobStorageCredentialsDto, AzureBlobStorageCredentials> createAzureCredentials() {

    AzureBlobSecretKeySelector secretKeySelector = new AzureBlobSecretKeySelector();
    AzureBlobSecretKeySelectorDto secretKeySelectorDto = new AzureBlobSecretKeySelectorDto();

    final var accessKey = generateSecretKeySelector();
    secretKeySelector.setAccessKey(accessKey);
    secretKeySelectorDto.setAccessKey(accessKey);

    final var account = generateSecretKeySelector();
    secretKeySelector.setAccount(account);
    secretKeySelectorDto.setAccount(account);

    AzureBlobStorageCredentialsDto target = new AzureBlobStorageCredentialsDto();
    AzureBlobStorageCredentials source = new AzureBlobStorageCredentials();
    target.setSecretKeySelectors(secretKeySelectorDto);
    source.setSecretKeySelectors(secretKeySelector);

    return new TransformerTuple<>(target, source);
  }

  private static SecretKeySelector generateSecretKeySelector() {

    final SecretKeySelector secretKeySelector = new SecretKeySelector();
    secretKeySelector.setKey(StringUtils.getRandomString());
    secretKeySelector.setName(StringUtils.getRandomString());
    return secretKeySelector;
  }

  private static TransformerTuple<
      GoogleCloudSecretKeySelectorDto,
      GoogleCloudSecretKeySelector> createGoogleCloudSecretKeySelector() {

    var dto = new GoogleCloudSecretKeySelectorDto();
    var crd = new GoogleCloudSecretKeySelector();

    var secretKeySelector = generateSecretKeySelector();
    dto.setServiceAccountJsonKey(secretKeySelector);
    crd.setServiceAccountJsonKey(secretKeySelector);

    return new TransformerTuple<>(dto, crd);
  }

  @Test
  void testS3Transformation() {

    var tuple = createS3BackupStorage();
    TransformerTestUtil.assertTransformation(storageTransformer, tuple);
  }

  @Test
  void testS3CompatibleTransformation() {

    var tuple = createS3CompatibleBackupStorage();
    TransformerTestUtil.assertTransformation(storageTransformer, tuple);

  }

  @Test
  void testGcsTransformation() {

    var tuple = createGcsBackupStorage();
    TransformerTestUtil.assertTransformation(storageTransformer, tuple);

  }

  @Test
  void testAzureTransformation() {

    var tuple = createAzureBackupStorage();
    TransformerTestUtil.assertTransformation(storageTransformer, tuple);
  }
}
