/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.ShardedBackupTransformer;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupResourceTest extends AbstractCustomResourceTest
      <ShardedBackupDto, StackGresShardedBackup,
      ShardedBackupResource, NamespacedShardedBackupResource> {

  @Override
  protected DefaultKubernetesResourceList<StackGresShardedBackup> getCustomResourceList() {
    return Fixtures.shardedBackupList().loadDefault().withJustFirstElement().get();
  }

  @Override
  protected ShardedBackupDto getDto() {
    return DtoFixtures.shardedBackup().loadDefault().get();
  }

  @Override
  protected AbstractResourceTransformer<ShardedBackupDto, StackGresShardedBackup> getTransformer() {
    final JsonMapper mapper = JsonMapper.builder().build();
    return new ShardedBackupTransformer(mapper);
  }

  @Override
  protected ShardedBackupResource getService() {
    return new ShardedBackupResource();
  }

  @Override
  protected NamespacedShardedBackupResource getNamespacedService() {
    return new NamespacedShardedBackupResource();
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
  protected void checkDto(ShardedBackupDto dto, StackGresShardedBackup resource) {
    assertNotNull(dto.getMetadata());
    assertEquals("postgresql", dto.getMetadata().getNamespace());
    assertEquals("test", dto.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", dto.getMetadata().getUid());
    assertNotNull(dto.getSpec());
    assertEquals("stackgres", dto.getSpec().getSgShardedCluster());
    assertEquals(false, dto.getSpec().getManagedLifecycle());
    assertNotNull(dto.getStatus());
    assertEquals(6686407, dto.getStatus().getBackupInformation().getSize().getCompressed());
    assertEquals("", dto.getStatus().getProcess().getFailure());
    assertEquals("2020-01-10T08:56:09.098819Z", dto.getStatus().getProcess().getTiming().getEnd());
    assertEquals("110006", dto.getStatus().getBackupInformation().getPostgresVersion());
    assertEquals("Completed", dto.getStatus().getProcess().getStatus());
    assertEquals("backup-with-default-storage-backup-1578646560-mr7pg",
        dto.getStatus().getProcess().getJobPod());
    assertEquals("2020-01-10T08:56:06.879707Z",
        dto.getStatus().getProcess().getTiming().getStart());
    assertNull(dto.getStatus().getTested());
    assertEquals("2020-01-10T08:56:09.119Z", dto.getStatus().getProcess().getTiming().getStored());
    assertEquals(24037855, dto.getStatus().getBackupInformation().getSize().getUncompressed());
  }

  @Override
  protected void checkCustomResource(StackGresShardedBackup resource, ShardedBackupDto resourceDto,
      Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("test", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("stackgres", resource.getSpec().getSgShardedCluster());
    assertEquals(false, resource.getSpec().getManagedLifecycle());
    switch (operation) {
      case UPDATE:
        assertNotNull(resource.getStatus());
        assertEquals(6686407,
            resource.getStatus().getBackupInformation().getSize().getCompressed());
        assertEquals("", resource.getStatus().getProcess().getFailure());
        assertEquals("2020-01-10T08:56:09.098819Z",
            resource.getStatus().getProcess().getTiming().getEnd());
        assertEquals("110006", resource.getStatus().getBackupInformation().getPostgresVersion());
        assertEquals("Completed", resource.getStatus().getProcess().getStatus());
        assertEquals("backup-with-default-storage-backup-1578646560-mr7pg",
            resource.getStatus().getProcess().getJobPod());
        assertEquals("2020-01-10T08:56:06.879707Z",
            resource.getStatus().getProcess().getTiming().getStart());
        assertNull(resource.getStatus().getTested());
        assertEquals("2020-01-10T08:56:09.119Z",
            resource.getStatus().getProcess().getTiming().getStored());
        assertEquals(24037855,
            resource.getStatus().getBackupInformation().getSize().getUncompressed());
        return;
      default:
        assertNull(resource.getStatus());
        return;
    }
  }

}
