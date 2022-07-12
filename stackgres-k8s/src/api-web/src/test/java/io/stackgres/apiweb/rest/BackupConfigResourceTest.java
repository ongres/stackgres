/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.BackupConfigTransformer;
import io.stackgres.apiweb.transformer.BackupStorageTransformer;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupConfigResourceTest extends AbstractDependencyCustomResourceTest
      <BackupConfigDto, StackGresBackupConfig,
      BackupConfigResource, NamespacedBackupConfigResource> {

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private ResourceWriter<Secret> secretWriter;

  @BeforeEach
  @Override
  void setUp() {
    super.setUp();
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
  protected AbstractDependencyResourceTransformer<BackupConfigDto, StackGresBackupConfig>
      getTransformer() {
    var mapper = JsonMapper.builder().build();
    return new BackupConfigTransformer(
        new BackupStorageTransformer(mapper), mapper);
  }

  @Override
  protected BackupConfigResource getService() {
    final BackupConfigResource backupConfigResource = new BackupConfigResource();
    backupConfigResource.secretFinder = secretFinder;
    backupConfigResource.secretWriter = secretWriter;
    return backupConfigResource;
  }

  @Override
  protected NamespacedBackupConfigResource getNamespacedService() {
    final NamespacedBackupConfigResource backupConfigResource =
        new NamespacedBackupConfigResource();
    backupConfigResource.secretFinder = secretFinder;
    backupConfigResource.backupConfigResource = getService();
    return backupConfigResource;
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
  void listShouldReturnAllDtos() {
    super.listShouldReturnAllDtos();
  }

  @Test
  @Override
  void getOfAnExistingDtoShouldReturnTheExistingDto() {
    super.getOfAnExistingDtoShouldReturnTheExistingDto();
  }

  @Test
  void createBackupConfigWithGoogleIdentity_shouldNotFail() {

    BackupConfigDto backupConfigDto = JsonUtil
        .readFromJson("backup_config/google_identity_config.json", BackupConfigDto.class);

    resourceDto = backupConfigDto;

    service.create(resourceDto);

  }

  @Override
  protected void checkDto(BackupConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("backupconf", resource.getMetadata().getName());
    assertEquals("93bc7621-0236-11ea-a1d5-0242ac110003", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("lz4", resource.getSpec().getBaseBackups().getCompressionMethod());
    assertNull(resource.getSpec().getBaseBackups().getPerformance().getMaxDiskBandwidth());
    assertNull(resource.getSpec().getBaseBackups().getPerformance().getMaxNetworkBandwidth());
    assertEquals(1,
        resource.getSpec().getBaseBackups().getPerformance().getUploadDiskConcurrency());
    assertEquals("*/1 * * * *", resource.getSpec().getBaseBackups().getCronSchedule());
    assertEquals(5, resource.getSpec().getBaseBackups().getRetention());
    assertNotNull(resource.getSpec().getStorage());
    assertNull(resource.getSpec().getStorage().getAzureBlob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertEquals("s3Compatible", resource.getSpec().getStorage().getType());
    assertNull(resource.getSpec().getStorage().getAzureBlob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertNull(resource.getSpec().getStorage().getS3());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getCredentials());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getAccessKeyId());
    assertEquals("minio", resource.getSpec().getStorage().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getAccessKeyId().getName());
    assertEquals("accesskey", resource.getSpec().getStorage().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getAccessKeyId().getKey());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getSecretAccessKey());
    assertEquals("minio", resource.getSpec().getStorage().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getSecretAccessKey().getName());
    assertEquals("secretkey", resource.getSpec().getStorage().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getSecretAccessKey().getKey());
    assertEquals("http://minio.stackgres:9000",
        resource.getSpec().getStorage().getS3Compatible().getEndpoint());
    assertEquals("stackgres", resource.getSpec().getStorage().getS3Compatible().getBucket());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getPath());
    assertEquals("k8s", resource.getSpec().getStorage().getS3Compatible().getRegion());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getStorageClass());
    assertTrue(resource.getSpec().getStorage().getS3Compatible().isForcePathStyle());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(1, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
  }

  @Override
  protected void checkCustomResource(StackGresBackupConfig resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("backupconf", resource.getMetadata().getName());
    assertEquals("93bc7621-0236-11ea-a1d5-0242ac110003", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("lz4", resource.getSpec().getBaseBackups().getCompression());
    assertNull(resource.getSpec().getBaseBackups().getPerformance().getMaxDiskBandwidth());
    assertNull(resource.getSpec().getBaseBackups().getPerformance().getMaxNetworkBandwidth());
    assertEquals(1,
        resource.getSpec().getBaseBackups().getPerformance().getUploadDiskConcurrency());
    assertEquals("*/1 * * * *", resource.getSpec().getBaseBackups().getCronSchedule());
    assertEquals(5, resource.getSpec().getBaseBackups().getRetention());
    assertNotNull(resource.getSpec().getStorage());
    assertNull(resource.getSpec().getStorage().getAzureBlob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertEquals("s3Compatible", resource.getSpec().getStorage().getType());
    assertNull(resource.getSpec().getStorage().getAzureBlob());
    assertNull(resource.getSpec().getStorage().getGcs());
    assertNull(resource.getSpec().getStorage().getS3());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getAwsCredentials());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getAwsCredentials()
        .getSecretKeySelectors().getAccessKeyId());
    assertEquals("backupconf-secrets", resource.getSpec().getStorage().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors().getAccessKeyId().getName());
    assertEquals(BackupConfigResourceUtil.S3COMPATIBLE_ACCESS_KEY,
        resource.getSpec().getStorage().getS3Compatible().getAwsCredentials()
            .getSecretKeySelectors().getAccessKeyId().getKey());
    assertNotNull(resource.getSpec().getStorage().getS3Compatible().getAwsCredentials()
        .getSecretKeySelectors().getSecretAccessKey());
    assertEquals("backupconf-secrets", resource.getSpec().getStorage().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors().getSecretAccessKey().getName());
    assertEquals(BackupConfigResourceUtil.S3COMPATIBLE_SECRET_KEY,
        resource.getSpec().getStorage().getS3Compatible().getAwsCredentials()
            .getSecretKeySelectors().getSecretAccessKey().getKey());
    assertEquals("http://minio.stackgres:9000",
        resource.getSpec().getStorage().getS3Compatible().getEndpoint());
    assertEquals("stackgres", resource.getSpec().getStorage().getS3Compatible().getBucket());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getPath());
    assertEquals("s3://stackgres", resource.getSpec().getStorage().getS3Compatible().getPrefix());
    assertEquals("k8s", resource.getSpec().getStorage().getS3Compatible().getRegion());
    assertNull(resource.getSpec().getStorage().getS3Compatible().getStorageClass());
    assertTrue(resource.getSpec().getStorage().getS3Compatible().isForcePathStyle());
  }

}
