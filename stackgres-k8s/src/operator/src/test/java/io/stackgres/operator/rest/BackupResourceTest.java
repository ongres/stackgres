/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.backup.BackupDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.BackupConfigTransformer;
import io.stackgres.operator.rest.transformer.BackupTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupResourceTest extends AbstractCustomResourceTest<BackupDto, StackGresBackup> {

  @Override
  protected CustomResourceList<StackGresBackup> getCustomResourceList() {
    return JsonUtil.readFromJson("stackgres_backup/list.json", StackGresBackupList.class);
  }

  @Override
  protected BackupDto getResourceDto() {
    return JsonUtil.readFromJson("stackgres_backup/dto.json", BackupDto.class);
  }

  @Override
  protected AbstractResourceTransformer<BackupDto, StackGresBackup> getTransformer() {
    final BackupTransformer backupTransformer = new BackupTransformer();
    backupTransformer.setBackupConfigTransformer(new BackupConfigTransformer());
    return backupTransformer;
  }

  @Override
  protected AbstractRestService<BackupDto, StackGresBackup> getService(
      CustomResourceScanner<StackGresBackup> scanner, CustomResourceFinder<StackGresBackup> finder,
      CustomResourceScheduler<StackGresBackup> scheduler,
      AbstractResourceTransformer<BackupDto, StackGresBackup> transformer) {
    return new BackupResource(scanner, finder, scheduler, transformer);
  }

  @Override
  protected String getResourceNamespace() {
    return "postgresql";
  }

  @Override
  protected String getResourceName() {
    return "test";
  }

  @Override
  protected void checkBackupConfig(BackupDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("test", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("stackgres", resource.getSpec().getCluster());
    assertEquals(false, resource.getSpec().getIsPermanent());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getBackupConfig());
    assertEquals("lz4", resource.getStatus().getBackupConfig().getCompressionMethod());
    assertEquals(0, resource.getStatus().getBackupConfig().getDiskRateLimit());
    assertNull(resource.getStatus().getBackupConfig().getFullSchedule());
    assertEquals(0, resource.getStatus().getBackupConfig().getFullWindow());
    assertEquals(0, resource.getStatus().getBackupConfig().getNetworkRateLimit());
    assertNull(resource.getStatus().getBackupConfig().getPgpConfiguration());
    assertEquals(0, resource.getStatus().getBackupConfig().getRetention());
    assertNotNull(resource.getStatus().getBackupConfig().getStorage());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getAzureblob());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getGcs());
    assertEquals("s3compatible", resource.getStatus().getBackupConfig().getStorage().getType());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getAzureblob());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getGcs());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3());
    assertNotNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible());
    assertNotNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible().getCredentials());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible().getCseKmsId());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible().getCseKmsRegion());
    assertEquals("http://minio.stackgres.svc:9000", resource.getStatus().getBackupConfig().getStorage().getS3compatible().getEndpoint());
    assertEquals("s3://stackgres", resource.getStatus().getBackupConfig().getStorage().getS3compatible().getPrefix());
    assertEquals("k8s", resource.getStatus().getBackupConfig().getStorage().getS3compatible().getRegion());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible().getSse());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible().getSseKmsId());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3compatible().getStorageClass());
    assertTrue(resource.getStatus().getBackupConfig().getStorage().getS3compatible().isForcePathStyle());
    assertEquals(6686407, resource.getStatus().getCompressedSize());
    assertNull(resource.getStatus().getControlData());
    assertEquals("/var/lib/postgresql/data", resource.getStatus().getDataDir());
    assertEquals("", resource.getStatus().getFailureReason());
    assertEquals("218104056", resource.getStatus().getFinishLsn());
    assertEquals("2020-01-10T08:56:09.098819Z", resource.getStatus().getFinishTime());
    assertEquals("backup-with-default-storage-1", resource.getStatus().getHostname());
    assertEquals(false, resource.getStatus().getIsPermanent());
    assertEquals("base_00000002000000000000000D", resource.getStatus().getName());
    assertEquals("110006", resource.getStatus().getPgVersion());
    assertEquals("Completed", resource.getStatus().getPhase());
    assertEquals("backup-with-default-storage-backup-1578646560-mr7pg", resource.getStatus().getPod());
    assertEquals("218103848", resource.getStatus().getStartLsn());
    assertEquals("2020-01-10T08:56:06.879707Z", resource.getStatus().getStartTime());
    assertEquals("6780234708837765169", resource.getStatus().getSystemIdentifier());
    assertNull(resource.getStatus().getTested());
    assertEquals("2020-01-10T08:56:09.119Z", resource.getStatus().getTime());
    assertEquals(24037855, resource.getStatus().getUncompressedSize());
    assertEquals("00000002000000000000000D", resource.getStatus().getWalFileName());
  }

  @Override
  protected void checkBackupConfig(StackGresBackup resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("test", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
   assertNotNull(resource.getSpec());
    assertEquals("stackgres", resource.getSpec().getCluster());
    assertEquals(false, resource.getSpec().getIsPermanent());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getBackupConfig());
    assertEquals("lz4", resource.getStatus().getBackupConfig().getCompressionMethod());
    assertEquals(0, resource.getStatus().getBackupConfig().getDiskRateLimit());
    assertNull(resource.getStatus().getBackupConfig().getFullSchedule());
    assertEquals(0, resource.getStatus().getBackupConfig().getFullWindow());
    assertEquals(0, resource.getStatus().getBackupConfig().getNetworkRateLimit());
    assertNull(resource.getStatus().getBackupConfig().getPgpConfiguration());
    assertEquals(0, resource.getStatus().getBackupConfig().getRetention());
    assertNotNull(resource.getStatus().getBackupConfig().getStorage());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getAzureblob());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getGcs());
    assertEquals("s3", resource.getStatus().getBackupConfig().getStorage().getType());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getAzureblob());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getGcs());
    assertNotNull(resource.getStatus().getBackupConfig().getStorage().getS3());
    assertNotNull(resource.getStatus().getBackupConfig().getStorage().getS3().getCredentials());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3().getCseKmsId());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3().getCseKmsRegion());
    assertEquals("http://minio.stackgres.svc:9000", resource.getStatus().getBackupConfig().getStorage().getS3().getEndpoint());
    assertEquals("s3://stackgres", resource.getStatus().getBackupConfig().getStorage().getS3().getPrefix());
    assertEquals("k8s", resource.getStatus().getBackupConfig().getStorage().getS3().getRegion());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3().getSse());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3().getSseKmsId());
    assertNull(resource.getStatus().getBackupConfig().getStorage().getS3().getStorageClass());
    assertTrue(resource.getStatus().getBackupConfig().getStorage().getS3().isForcePathStyle());
    assertEquals(6686407, resource.getStatus().getCompressedSize());
    assertNull(resource.getStatus().getControlData());
    assertEquals("/var/lib/postgresql/data", resource.getStatus().getDataDir());
    assertEquals("", resource.getStatus().getFailureReason());
    assertEquals("218104056", resource.getStatus().getFinishLsn());
    assertEquals("2020-01-10T08:56:09.098819Z", resource.getStatus().getFinishTime());
    assertEquals("backup-with-default-storage-1", resource.getStatus().getHostname());
    assertEquals(false, resource.getStatus().getIsPermanent());
    assertEquals("base_00000002000000000000000D", resource.getStatus().getName());
    assertEquals("110006", resource.getStatus().getPgVersion());
    assertEquals("Completed", resource.getStatus().getPhase());
    assertEquals("backup-with-default-storage-backup-1578646560-mr7pg", resource.getStatus().getPod());
    assertEquals("218103848", resource.getStatus().getStartLsn());
    assertEquals("2020-01-10T08:56:06.879707Z", resource.getStatus().getStartTime());
    assertEquals("6780234708837765169", resource.getStatus().getSystemIdentifier());
    assertNull(resource.getStatus().getTested());
    assertEquals("2020-01-10T08:56:09.119Z", resource.getStatus().getTime());
    assertEquals(24037855, resource.getStatus().getUncompressedSize());
    assertEquals("00000002000000000000000D", resource.getStatus().getWalFileName());
  }

}