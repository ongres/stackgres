/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigSpec;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigStatus;
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
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
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
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BackupConfigTransformerTest {

  @Inject
  BackupConfigTransformer transformer;

  public static TransformerTuple<BackupConfigDto, StackGresBackupConfig> createS3BackupConfig() {
    StackGresBackupConfig source = new StackGresBackupConfig();
    BackupConfigDto target = new BackupConfigDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupConfigSpec.class,
            StackGresBackupConfigSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var storage = createS3BackupStorage();
    source.getSpec().setStorage(storage.source());
    target.getSpec().setStorage(storage.target());

    target.setStatus(new BackupConfigStatus());
    target.getStatus().setClusters(List.of(StringUtils.getRandomClusterName()));

    return new TransformerTuple<>(target, source);
  }

  public static TransformerTuple<BackupConfigDto, StackGresBackupConfig>
      createS3CompatibleBackupConfig() {
    StackGresBackupConfig source = new StackGresBackupConfig();
    BackupConfigDto target = new BackupConfigDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupConfigSpec.class,
            StackGresBackupConfigSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var storage = createS3CompatibleBackupStorage();
    source.getSpec().setStorage(storage.source());
    target.getSpec().setStorage(storage.target());

    target.setStatus(new BackupConfigStatus());
    target.getStatus().setClusters(List.of(StringUtils.getRandomClusterName()));

    return new TransformerTuple<>(target, source);
  }

  public static TransformerTuple<BackupConfigDto, StackGresBackupConfig> createGcsBackupConfig() {
    StackGresBackupConfig source = new StackGresBackupConfig();
    BackupConfigDto target = new BackupConfigDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupConfigSpec.class,
            StackGresBackupConfigSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var storage = createGcsBackupStorage();
    source.getSpec().setStorage(storage.source());
    target.getSpec().setStorage(storage.target());

    target.setStatus(new BackupConfigStatus());
    target.getStatus().setClusters(List.of(StringUtils.getRandomClusterName()));

    return new TransformerTuple<>(target, source);
  }

  public static TransformerTuple<BackupConfigDto, StackGresBackupConfig> createAzureBackupConfig() {
    StackGresBackupConfig source = new StackGresBackupConfig();
    BackupConfigDto target = new BackupConfigDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = TransformerTestUtil
        .fillTupleWithRandomData(
            BackupConfigSpec.class,
            StackGresBackupConfigSpec.class
        );
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var storage = createAzureBackupStorage();
    source.getSpec().setStorage(storage.source());
    target.getSpec().setStorage(storage.target());

    target.setStatus(new BackupConfigStatus());
    target.getStatus().setClusters(List.of(StringUtils.getRandomClusterName()));

    return new TransformerTuple<>(target, source);
  }

  public static TransformerTuple<BackupStorageDto, BackupStorage> createS3BackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "s3";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createS3Storage();
    crdBackupStorage.setS3(s3Storage.source());
    dtoBackupStorage.setS3(s3Storage.target());

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
    crdBackupStorage.setS3Compatible(s3Storage.source());
    dtoBackupStorage.setS3Compatible(s3Storage.target());

    return new TransformerTuple<>(dtoBackupStorage, crdBackupStorage);
  }

  public static TransformerTuple<BackupStorageDto, BackupStorage> createGcsBackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "gcs";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createGcsStorage();
    crdBackupStorage.setGcs(s3Storage.source());
    dtoBackupStorage.setGcs(s3Storage.target());

    return new TransformerTuple<>(dtoBackupStorage, crdBackupStorage);
  }

  public static TransformerTuple<BackupStorageDto, BackupStorage> createAzureBackupStorage() {
    BackupStorage crdBackupStorage = new BackupStorage();
    BackupStorageDto dtoBackupStorage = new BackupStorageDto();

    final String type = "azureBlob";
    crdBackupStorage.setType(type);
    dtoBackupStorage.setType(type);

    var s3Storage = createAzureBlobStorage();
    crdBackupStorage.setAzureBlob(s3Storage.source());
    dtoBackupStorage.setAzureBlob(s3Storage.target());

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
    azureBlobStorageDto.setAzureCredentials(azureCredentials.target());
    azureBlobStorage.setAzureCredentials(azureCredentials.source());

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
    crdS3Storage.setAwsCredentials(credentialsTuple.source());
    dtoS3Storage.setAwsCredentials(credentialsTuple.target());

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

    boolean enablePathStyleAddressing = new Random().nextBoolean();
    target.setEnablePathStyleAddressing(enablePathStyleAddressing);
    source.setEnablePathStyleAddressing(enablePathStyleAddressing);

    final var credentialTuple = generateAwsCredentials();

    target.setAwsCredentials(credentialTuple.target());
    source.setAwsCredentials(credentialTuple.source());

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
    crdS3Storage.setGcpCredentials(credentialsTuple.source());
    dtoS3Storage.setGcpCredentials(credentialsTuple.target());

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
    crdCredentials.setSecretKeySelectors(selector.source());
    dtoCredentials.setSecretKeySelectors(selector.target());

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
    secretKeySelector.setStorageAccount(account);
    secretKeySelectorDto.setStorageAccount(account);

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
    var tuple = createS3BackupConfig();

    final List<String> clusters = Optional.of(tuple.target())
        .map(BackupConfigDto::getStatus)
        .map(BackupConfigStatus::getClusters).orElse(List.of());

    TransformerTestUtil.assertTransformation(transformer, tuple, clusters);
  }

  @Test
  void testS3CompatibleTransformation() {
    var tuple = createS3CompatibleBackupConfig();

    final List<String> clusters = Optional.of(tuple.target())
        .map(BackupConfigDto::getStatus)
        .map(BackupConfigStatus::getClusters).orElse(List.of());

    TransformerTestUtil.assertTransformation(transformer, tuple, clusters);
  }

  @Test
  void testGcsTransformation() {
    var tuple = createGcsBackupConfig();

    final List<String> clusters = Optional.of(tuple.target())
        .map(BackupConfigDto::getStatus)
        .map(BackupConfigStatus::getClusters).orElse(List.of());

    TransformerTestUtil.assertTransformation(transformer, tuple, clusters);
  }

  @Test
  void testAzureTransformation() {
    var tuple = createAzureBackupConfig();

    final List<String> clusters = Optional.of(tuple.target())
        .map(BackupConfigDto::getStatus)
        .map(BackupConfigStatus::getClusters).orElse(List.of());

    TransformerTestUtil.assertTransformation(transformer, tuple, clusters);
  }
}
