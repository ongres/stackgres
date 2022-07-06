/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.DistributedLogsTransformer;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsResourceTest
    extends AbstractDependencyCustomResourceTest<DistributedLogsDto, StackGresDistributedLogs,
    DistributedLogsResource, NamespacedDistributedLogsResource> {

  @Override
  protected CustomResourceList<StackGresDistributedLogs> getCustomResourceList() {
    return JsonUtil
        .readFromJson("distributedlogs/list.json", StackGresDistributedLogsList.class);
  }

  @Override
  protected DistributedLogsDto getResourceDto() {
    return JsonUtil
        .readFromJson("distributedlogs/dto.json", DistributedLogsDto.class);
  }

  @Override
  protected AbstractDependencyResourceTransformer<DistributedLogsDto, StackGresDistributedLogs>
      getTransformer() {
    return new DistributedLogsTransformer(
        JsonMapper.builder().build()
    );
  }

  @Override
  protected DistributedLogsResource getService() {
    return new DistributedLogsResource();
  }

  @Override
  protected NamespacedDistributedLogsResource getNamespacedService() {
    return new NamespacedDistributedLogsResource();
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "distributedlogs";
  }

  @Override
  protected void checkDto(DistributedLogsDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("distributedlogs", resource.getMetadata().getName());
    assertEquals("008af052-7fcd-4665-b3b9-6d7dedbc543c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertNull(resource.getSpec().getNonProduction());
    assertNotNull(resource.getSpec().getPersistentVolume());
    assertEquals("128Mi", resource.getSpec().getPersistentVolume().getSize());
    assertNull(resource.getSpec().getPersistentVolume().getStorageClass());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(1, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
  }

  @Override
  protected void checkCustomResource(StackGresDistributedLogs resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("distributedlogs", resource.getMetadata().getName());
    assertEquals("008af052-7fcd-4665-b3b9-6d7dedbc543c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertNull(resource.getSpec().getNonProductionOptions());
    assertNotNull(resource.getSpec().getPersistentVolume());
    assertEquals("128Mi", resource.getSpec().getPersistentVolume().getSize());
    assertNull(resource.getSpec().getPersistentVolume().getStorageClass());
  }

}
