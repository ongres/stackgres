/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.ResourceFinder;
import io.stackgres.operator.resource.ResourceWriter;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.BackupConfigTransformer;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigResourceTest
    extends AbstractCustomResourceTest<BackupConfigDto, StackGresBackupConfig> {

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private ResourceWriter<Secret> secretWriter;

  private Secret secret;

  @BeforeEach
  @Override
  void setUp() {
    super.setUp();

    secret = new SecretBuilder()
        .withNewMetadata()
        .withNamespace(getResourceNamespace())
        .withName("minio")
        .endMetadata()
        .withData(ImmutableMap.of(
            "accesskey", ResourceUtil.encodeSecret("test"),
            "secretkey", ResourceUtil.encodeSecret("test")))
        .build();
  }

  @Override
  protected CustomResourceList<StackGresBackupConfig> getCustomResourceList() {
    return JsonUtil
        .readFromJson("backup_config/list.json", StackGresBackupConfigList.class);
  }

  @Override
  protected BackupConfigDto getResourceDto() {
    return JsonUtil
        .readFromJson("backup_config/dto.json", BackupConfigDto.class);
  }

  @Override
  protected AbstractResourceTransformer<BackupConfigDto, StackGresBackupConfig> getTransformer() {
    return new BackupConfigTransformer();
  }

  @Override
  protected AbstractRestService<BackupConfigDto, StackGresBackupConfig> getService(
      CustomResourceScanner<StackGresBackupConfig> scanner,
      CustomResourceFinder<StackGresBackupConfig> finder,
      CustomResourceScheduler<StackGresBackupConfig> scheduler,
      AbstractResourceTransformer<BackupConfigDto, StackGresBackupConfig> transformer) {
    return new BackupConfigResource(scanner, finder, scheduler, transformer,
        secretFinder, secretWriter);
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "backupconf";
  }

  @Test
  @Override
  void listShouldReturnAllBackupConfigs() {
    when(secretFinder.findByNameAndNamespace(anyString(), anyString())).thenReturn(Optional.of(secret));

    super.listShouldReturnAllBackupConfigs();
  }

  @Test
  @Override
  void getOfAnExistingBackupConfigShouldReturnTheExistingBackupConfig() {
    when(secretFinder.findByNameAndNamespace(anyString(), anyString())).thenReturn(Optional.of(secret));

    super.getOfAnExistingBackupConfigShouldReturnTheExistingBackupConfig();
  }

  @Override
  protected void checkBackupConfig(BackupConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("backupconf", resource.getMetadata().getName());
    assertEquals("93bc7621-0236-11ea-a1d5-0242ac110003", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("lz4", resource.getSpec().getBaseBackup().getCompressionMethod());
    assertEquals(0, resource.getSpec().getBaseBackup().getPerformance().getDiskRateLimit());
    assertEquals("*/1 * * * *", resource.getSpec().getBaseBackup().getFullSchedule());
    assertEquals(0, resource.getSpec().getBaseBackup().getPerformance().getNetworkRateLimit());
    assertEquals(5, resource.getSpec().getBaseBackup().getRetention());
    assertNotNull(resource.getSpec().getStorage());
    assertNull(resource.getSpec().getStorage().getAzureblob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertEquals("s3compatible", resource.getSpec().getStorage().getType());
    assertNull(resource.getSpec().getStorage().getAzureblob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertNull(resource.getSpec().getStorage().getS3());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getCredentials());
    assertEquals("test", resource.getSpec().getStorage().getS3Compatible().getCredentials().getAccessKey());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKeySelectors().getAccessKeyId());
    assertEquals("minio", resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKeySelectors().getAccessKeyId().getName());
    assertEquals("accesskey", resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKeySelectors().getAccessKeyId().getKey());
    assertEquals("test", resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKey());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKeySelectors().getSecretAccessKey());
    assertEquals("minio", resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKeySelectors().getSecretAccessKey().getName());
    assertEquals("secretkey", resource.getSpec().getStorage().getS3Compatible().getCredentials().getSecretKeySelectors().getSecretAccessKey().getKey());
    assertEquals("http://minio.stackgres.svc:9000",
        resource.getSpec().getStorage().getS3Compatible().getEndpoint());
    assertEquals("stackgres", resource.getSpec().getStorage().getS3Compatible().getBucket());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getPath());
    assertEquals("k8s", resource.getSpec().getStorage().getS3Compatible().getRegion());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getStorageClass());
    assertTrue(resource.getSpec().getStorage().getS3Compatible().isForcePathStyle());
  }

  @Override
  protected void checkBackupConfig(StackGresBackupConfig resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("backupconf", resource.getMetadata().getName());
    assertEquals("93bc7621-0236-11ea-a1d5-0242ac110003", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("lz4", resource.getSpec().getBaseBackups().getCompression());
    assertEquals(0, resource.getSpec().getBaseBackups().getPerformance().getMaxDiskBandwitdh());
    assertEquals("*/1 * * * *", resource.getSpec().getBaseBackups().getCronSchedule());
    assertEquals(0, resource.getSpec().getBaseBackups().getPerformance().getMaxNetworkBandwitdh());
    assertEquals(5, resource.getSpec().getBaseBackups().getRetention());
    assertNotNull(resource.getSpec().getStorage());
    assertNull(resource.getSpec().getStorage().getAzureblob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertEquals("s3compatible", resource.getSpec().getStorage().getType());
    assertNull(resource.getSpec().getStorage().getAzureblob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertNull(resource.getSpec().getStorage().getS3());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getAwsCredentials());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getAwsCredentials().getSecretKeySelectors().getAccessKeyId());
    assertEquals("backupconf-secrets", resource.getSpec().getStorage().getS3Compatible().getAwsCredentials().getSecretKeySelectors().getAccessKeyId().getName());
    assertEquals(BackupConfigResourceUtil.S3COMPATIBLE_ACCESS_KEY,
        resource.getSpec().getStorage().getS3Compatible().getAwsCredentials().getSecretKeySelectors().getAccessKeyId().getKey());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getAwsCredentials().getSecretKeySelectors().getSecretAccessKey());
    assertEquals("backupconf-secrets", resource.getSpec().getStorage().getS3Compatible().getAwsCredentials().getSecretKeySelectors().getSecretAccessKey().getName());
    assertEquals(BackupConfigResourceUtil.S3COMPATIBLE_SECRET_KEY,
        resource.getSpec().getStorage().getS3Compatible().getAwsCredentials().getSecretKeySelectors().getSecretAccessKey().getKey());
    assertEquals("http://minio.stackgres.svc:9000", resource.getSpec().getStorage().getS3Compatible().getEndpoint());
    assertEquals("stackgres", resource.getSpec().getStorage().getS3Compatible().getBucket());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getPath());
    assertEquals("s3://stackgres", resource.getSpec().getStorage().getS3Compatible().getPrefix());
    assertEquals("k8s", resource.getSpec().getStorage().getS3Compatible().getRegion());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getStorageClass());
    assertTrue(resource.getSpec().getStorage().getS3Compatible().isForcePathStyle());
  }

}