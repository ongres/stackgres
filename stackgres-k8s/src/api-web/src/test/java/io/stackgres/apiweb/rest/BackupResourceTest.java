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
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.BackupConfigTransformer;
import io.stackgres.apiweb.transformer.BackupStorageTransformer;
import io.stackgres.apiweb.transformer.BackupTransformer;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupResourceTest extends AbstractCustomResourceTest
      <BackupDto, StackGresBackup, BackupResource, NamespacedBackupResource> {

  @Override
  protected CustomResourceList<StackGresBackup> getCustomResourceList() {
    return Fixtures.backupList().loadDefault().withJustFirstElement().get();
  }

  @Override
  protected BackupDto getDto() {
    return DtoFixtures.backup().loadDefault().get();
  }

  @Override
  protected AbstractResourceTransformer<BackupDto, StackGresBackup> getTransformer() {
    final JsonMapper mapper = JsonMapper.builder().build();
    return new BackupTransformer(
        new BackupConfigTransformer(
            new BackupStorageTransformer(mapper),
            mapper),
        mapper
    );
  }

  @Override
  protected BackupResource getService() {
    return new BackupResource();
  }

  @Override
  protected NamespacedBackupResource getNamespacedService() {
    return new NamespacedBackupResource();
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
  @SuppressWarnings("deprecation")
  protected void checkDto(BackupDto dto, StackGresBackup resource) {
    assertNotNull(dto.getMetadata());
    assertEquals("postgresql", dto.getMetadata().getNamespace());
    assertEquals("test", dto.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", dto.getMetadata().getUid());
    assertNotNull(dto.getSpec());
    assertEquals("stackgres", dto.getSpec().getCluster());
    assertEquals(false, dto.getSpec().getManagedLifecycle());
    assertNotNull(dto.getStatus());
    assertNotNull(dto.getStatus().getBackupConfig());
    assertEquals("lz4", dto.getStatus().getBackupConfig().getBaseBackups().getCompressionMethod());
    assertNull(
        dto.getStatus().getBackupConfig().getBaseBackups().getPerformance().getMaxDiskBandwidth());
    assertNull(dto.getStatus().getBackupConfig().getBaseBackups().getPerformance()
        .getMaxNetworkBandwidth());
    assertEquals(1, dto.getStatus().getBackupConfig().getBaseBackups().getPerformance()
        .getUploadDiskConcurrency());
    assertNull(dto.getStatus().getBackupConfig().getBaseBackups().getCronSchedule());
    assertNull(dto.getStatus().getBackupConfig().getBaseBackups().getRetention());
    assertNotNull(dto.getStatus().getBackupConfig().getStorage());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getAzureBlob());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getGcs());
    assertEquals("s3Compatible", dto.getStatus().getBackupConfig().getStorage().getType());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getAzureBlob());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getGcs());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getS3());
    assertNotNull(dto.getStatus().getBackupConfig().getStorage().getS3Compatible());
    assertNotNull(
        dto.getStatus().getBackupConfig().getStorage().getS3Compatible().getCredentials());
    assertEquals("http://minio.stackgres:9000",
        dto.getStatus().getBackupConfig().getStorage().getS3Compatible().getEndpoint());
    assertEquals("stackgres",
        dto.getStatus().getBackupConfig().getStorage().getS3Compatible().getBucket());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getS3Compatible().getPath());
    assertEquals("k8s",
        dto.getStatus().getBackupConfig().getStorage().getS3Compatible().getRegion());
    assertNull(dto.getStatus().getBackupConfig().getStorage().getS3Compatible().getStorageClass());
    assertTrue(dto.getStatus().getBackupConfig().getStorage().getS3Compatible().isForcePathStyle());
    assertEquals(6686407, dto.getStatus().getBackupInformation().getSize().getCompressed());
    assertNull(dto.getStatus().getBackupInformation().getControlData());
    assertEquals("/var/lib/postgresql/data", dto.getStatus().getBackupInformation().getPgData());
    assertEquals("", dto.getStatus().getProcess().getFailure());
    assertEquals("218104056", dto.getStatus().getBackupInformation().getLsn().getEnd());
    assertEquals("2020-01-10T08:56:09.098819Z", dto.getStatus().getProcess().getTiming().getEnd());
    assertEquals("backup-with-default-storage-1",
        dto.getStatus().getBackupInformation().getHostname());
    assertEquals(false, dto.getStatus().getProcess().getManagedLifecycle());
    assertEquals("base_00000002000000000000000D", dto.getStatus().getInternalName());
    assertEquals("110006", dto.getStatus().getBackupInformation().getPostgresVersion());
    assertEquals("Completed", dto.getStatus().getProcess().getStatus());
    assertEquals("backup-with-default-storage-backup-1578646560-mr7pg",
        dto.getStatus().getProcess().getJobPod());
    assertEquals("218103848", dto.getStatus().getBackupInformation().getLsn().getStart());
    assertEquals("2020-01-10T08:56:06.879707Z",
        dto.getStatus().getProcess().getTiming().getStart());
    assertEquals("6780234708837765169",
        dto.getStatus().getBackupInformation().getSystemIdentifier());
    assertNull(dto.getStatus().getTested());
    assertEquals("2020-01-10T08:56:09.119Z", dto.getStatus().getProcess().getTiming().getStored());
    assertEquals(24037855, dto.getStatus().getBackupInformation().getSize().getUncompressed());
    assertEquals("00000002000000000000000D",
        dto.getStatus().getBackupInformation().getStartWalFile());
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void checkCustomResource(StackGresBackup resource, BackupDto resourceDto,
      Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("test", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("stackgres", resource.getSpec().getSgCluster());
    assertEquals(false, resource.getSpec().getManagedLifecycle());
    switch (operation) {
      case UPDATE:
        assertNotNull(resource.getStatus());
        assertNotNull(resource.getStatus().getBackupConfig());
        assertEquals("lz4",
            resource.getStatus().getBackupConfig().getBaseBackups().getCompression());
        assertNull(resource.getStatus().getBackupConfig().getBaseBackups().getPerformance()
            .getMaxDiskBandwidth());
        assertNull(resource.getStatus().getBackupConfig().getBaseBackups().getPerformance()
            .getMaxNetworkBandwidth());
        assertEquals(1, resource.getStatus().getBackupConfig().getBaseBackups().getPerformance()
            .getUploadDiskConcurrency());
        assertNull(resource.getStatus().getBackupConfig().getBaseBackups().getCronSchedule());
        assertNull(resource.getStatus().getBackupConfig().getBaseBackups().getRetention());
        assertNotNull(resource.getStatus().getBackupConfig().getStorage());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getAzureBlob());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getGcs());
        assertEquals("s3Compatible", resource.getStatus().getBackupConfig().getStorage().getType());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getAzureBlob());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getGcs());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getS3());
        assertNotNull(resource.getStatus().getBackupConfig().getStorage().getS3Compatible());
        assertNotNull(resource.getStatus().getBackupConfig().getStorage().getS3Compatible()
            .getAwsCredentials());
        assertEquals("http://minio.stackgres:9000",
            resource.getStatus().getBackupConfig().getStorage().getS3Compatible().getEndpoint());
        assertEquals("stackgres",
            resource.getStatus().getBackupConfig().getStorage().getS3Compatible().getBucket());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getS3Compatible().getPath());
        assertEquals("k8s",
            resource.getStatus().getBackupConfig().getStorage().getS3Compatible().getRegion());
        assertNull(resource.getStatus().getBackupConfig().getStorage().getS3Compatible()
            .getStorageClass());
        assertTrue(resource.getStatus().getBackupConfig().getStorage().getS3Compatible()
            .isForcePathStyle());
        assertEquals(6686407,
            resource.getStatus().getBackupInformation().getSize().getCompressed());
        assertNull(resource.getStatus().getBackupInformation().getControlData());
        assertEquals("/var/lib/postgresql/data",
            resource.getStatus().getBackupInformation().getPgData());
        assertEquals("", resource.getStatus().getProcess().getFailure());
        assertEquals("218104056", resource.getStatus().getBackupInformation().getLsn().getEnd());
        assertEquals("2020-01-10T08:56:09.098819Z",
            resource.getStatus().getProcess().getTiming().getEnd());
        assertEquals("backup-with-default-storage-1",
            resource.getStatus().getBackupInformation().getHostname());
        assertEquals(false, resource.getStatus().getProcess().getManagedLifecycle());
        assertEquals("base_00000002000000000000000D", resource.getStatus().getInternalName());
        assertEquals("110006", resource.getStatus().getBackupInformation().getPostgresVersion());
        assertEquals("Completed", resource.getStatus().getProcess().getStatus());
        assertEquals("backup-with-default-storage-backup-1578646560-mr7pg",
            resource.getStatus().getProcess().getJobPod());
        assertEquals("218103848", resource.getStatus().getBackupInformation().getLsn().getStart());
        assertEquals("2020-01-10T08:56:06.879707Z",
            resource.getStatus().getProcess().getTiming().getStart());
        assertEquals("6780234708837765169",
            resource.getStatus().getBackupInformation().getSystemIdentifier());
        assertNull(resource.getStatus().getTested());
        assertEquals("2020-01-10T08:56:09.119Z",
            resource.getStatus().getProcess().getTiming().getStored());
        assertEquals(24037855,
            resource.getStatus().getBackupInformation().getSize().getUncompressed());
        assertEquals("00000002000000000000000D",
            resource.getStatus().getBackupInformation().getStartWalFile());
        return;
      default:
        assertNull(resource.getStatus());
        return;
    }
  }

}
