/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.DistributedLogsResource;
import io.stackgres.apiweb.distributedlogs.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.DistributedLogsTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsResourceTest
    extends AbstractDependencyCustomResourceTest<DistributedLogsDto, StackGresDistributedLogs,
    DistributedLogsResource> {

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
  protected AbstractDependencyResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> getTransformer() {
    return new DistributedLogsTransformer();
  }

  @Override
  protected DistributedLogsResource getService(
      CustomResourceScanner<StackGresDistributedLogs> scanner,
      CustomResourceFinder<StackGresDistributedLogs> finder,
      CustomResourceScheduler<StackGresDistributedLogs> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      AbstractDependencyResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> transformer) {
    return new DistributedLogsResource(scanner, finder, scheduler, clusterScanner, transformer);
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
    assertNotNull(resource.getSpec().getNonProduction());
    assertEquals(true, resource.getSpec().getNonProduction().getDisableClusterPodAntiAffinity());
    assertNotNull(resource.getSpec().getPersistentVolume());
    assertEquals("128Mi", resource.getSpec().getPersistentVolume().getVolumeSize());
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
    assertNotNull(resource.getSpec().getNonProduction());
    assertEquals(true, resource.getSpec().getNonProduction().getDisableClusterPodAntiAffinity());
    assertNotNull(resource.getSpec().getPersistentVolume());
    assertEquals("128Mi", resource.getSpec().getPersistentVolume().getVolumeSize());
    assertNull(resource.getSpec().getPersistentVolume().getStorageClass());
  }

}