/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.BackupStorageTransformer;
import io.stackgres.apiweb.transformer.ObjectStorageTransformer;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageList;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageObjectResourceTest extends AbstractDependencyCustomResourceTest
    <ObjectStorageDto, StackGresObjectStorage,
        ObjectStorageResource, NamespacedObjectStorageResource> {

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private ResourceWriter<Secret> secretWriter;

  private static ObjectStorageDto convertBackupConfigDtoToStorageObjectDto(
      BackupConfigDto backupConfigDto) {
    var resourceDto = new ObjectStorageDto();
    resourceDto.setMetadata(backupConfigDto.getMetadata());
    resourceDto.setSpec(backupConfigDto.getSpec().getStorage());
    return resourceDto;
  }

  private static StackGresObjectStorage convertBackupConfigToStorageObject(
      StackGresBackupConfig backupConfig) {
    var resourceDto = new StackGresObjectStorage();
    resourceDto.setMetadata(backupConfig.getMetadata());
    resourceDto.setSpec(backupConfig.getSpec().getStorage());
    return resourceDto;
  }

  @BeforeEach
  @Override
  void setUp() {
    super.setUp();
  }

  @Override
  protected CustomResourceList<StackGresObjectStorage> getCustomResourceList() {
    StackGresBackupConfigList stackGresBackupConfigList = JsonUtil
        .readFromJson("backup_config/list.json", StackGresBackupConfigList.class);

    final StackGresObjectStorageList stackGresObjectStorageList = new StackGresObjectStorageList();
    stackGresObjectStorageList.setMetadata(stackGresBackupConfigList.getMetadata());
    stackGresObjectStorageList.setItems(stackGresBackupConfigList.getItems().stream()
        .map(StorageObjectResourceTest::convertBackupConfigToStorageObject)
        .collect(Collectors.toList()));

    return stackGresObjectStorageList;
  }

  @Override
  protected ObjectStorageDto getResourceDto() {
    final BackupConfigDto backupConfigDto = JsonUtil
        .readFromJson("backup_config/dto.json", BackupConfigDto.class);
    final ObjectStorageDto objectStorageDto = new ObjectStorageDto();
    objectStorageDto.setMetadata(backupConfigDto.getMetadata());
    objectStorageDto.setSpec(backupConfigDto.getSpec().getStorage());
    return objectStorageDto;
  }

  @Override
  protected AbstractDependencyResourceTransformer<
      ObjectStorageDto,
      StackGresObjectStorage> getTransformer() {
    return new ObjectStorageTransformer(
        new BackupStorageTransformer(),
        new JsonMapper()
    );
  }

  @Override
  protected ObjectStorageResource getService() {
    final ObjectStorageResource backupConfigResource = new ObjectStorageResource();

    backupConfigResource.secretFinder = secretFinder;
    backupConfigResource.secretWriter = secretWriter;

    return backupConfigResource;
  }

  @Override
  protected NamespacedObjectStorageResource getNamespacedService() {
    final NamespacedObjectStorageResource backupConfigResource =
        new NamespacedObjectStorageResource();
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

    final BackupConfigDto backupConfigDto = JsonUtil
        .readFromJson("backup_config/google_identity_config.json", BackupConfigDto.class);

    this.resourceDto = convertBackupConfigDtoToStorageObjectDto(backupConfigDto);

    service.create(this.resourceDto);

  }

  @Override
  protected void checkDto(ObjectStorageDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("backupconf", resource.getMetadata().getName());
    assertEquals("93bc7621-0236-11ea-a1d5-0242ac110003", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertNull(resource.getSpec().getAzureBlob());
    assertNull(resource.getSpec().getGcs());
    assertEquals("s3Compatible", resource.getSpec().getType());
    assertNull(resource.getSpec().getAzureBlob());
    assertNull(resource.getSpec().getGcs());
    assertNull(resource.getSpec().getS3());
    assertNotNull(resource.getSpec().getS3Compatible());
    assertNotNull(resource.getSpec().getS3Compatible().getCredentials());
    assertNotNull(resource.getSpec().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getAccessKeyId());
    assertEquals("minio", resource.getSpec().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getAccessKeyId().getName());
    assertEquals("accesskey", resource.getSpec().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getAccessKeyId().getKey());
    assertNotNull(resource.getSpec().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getSecretAccessKey());
    assertEquals("minio", resource.getSpec().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getSecretAccessKey().getName());
    assertEquals("secretkey", resource.getSpec().getS3Compatible().getCredentials()
        .getSecretKeySelectors().getSecretAccessKey().getKey());
    assertEquals("http://minio.stackgres:9000",
        resource.getSpec().getS3Compatible().getEndpoint());
    assertEquals("stackgres", resource.getSpec().getS3Compatible().getBucket());
    assertNull(resource.getSpec().getS3Compatible().getPath());
    assertEquals("k8s", resource.getSpec().getS3Compatible().getRegion());
    assertNull(resource.getSpec().getS3Compatible().getStorageClass());
    assertTrue(resource.getSpec().getS3Compatible().isForcePathStyle());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(1, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
  }

  @Override
  protected void checkCustomResource(StackGresObjectStorage resource, Operation operation) {
    Assertions.assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("backupconf", resource.getMetadata().getName());
    assertEquals("93bc7621-0236-11ea-a1d5-0242ac110003", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertNull(resource.getSpec().getAzureBlob());
    assertNull(resource.getSpec().getGcs());
    assertEquals("s3Compatible", resource.getSpec().getType());
    assertNull(resource.getSpec().getAzureBlob());
    assertNull(resource.getSpec().getGcs());
    assertNull(resource.getSpec().getS3());
    assertNotNull(resource.getSpec().getS3Compatible());
    assertNotNull(resource.getSpec().getS3Compatible().getAwsCredentials());
    assertNotNull(resource.getSpec().getS3Compatible().getAwsCredentials()
        .getSecretKeySelectors().getAccessKeyId());
    assertEquals("backupconf-secrets", resource.getSpec().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors().getAccessKeyId().getName());
    assertEquals(BackupConfigResourceUtil.S3COMPATIBLE_ACCESS_KEY,
        resource.getSpec().getS3Compatible().getAwsCredentials()
            .getSecretKeySelectors().getAccessKeyId().getKey());
    assertNotNull(resource.getSpec().getS3Compatible().getAwsCredentials()
        .getSecretKeySelectors().getSecretAccessKey());
    assertEquals("backupconf-secrets", resource.getSpec().getS3Compatible()
        .getAwsCredentials().getSecretKeySelectors().getSecretAccessKey().getName());
    assertEquals(BackupConfigResourceUtil.S3COMPATIBLE_SECRET_KEY,
        resource.getSpec().getS3Compatible().getAwsCredentials()
            .getSecretKeySelectors().getSecretAccessKey().getKey());
    assertEquals("http://minio.stackgres:9000",
        resource.getSpec().getS3Compatible().getEndpoint());
    assertEquals("stackgres", resource.getSpec().getS3Compatible().getBucket());
    assertNull(resource.getSpec().getS3Compatible().getPath());
    assertEquals("s3://stackgres", resource.getSpec().getS3Compatible().getPrefix());
    assertEquals("k8s", resource.getSpec().getS3Compatible().getRegion());
    assertNull(resource.getSpec().getS3Compatible().getStorageClass());
    assertTrue(resource.getSpec().getS3Compatible().isForcePathStyle());
  }

}
