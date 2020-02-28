/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterPodConfigDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.ClusterTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterResourceTest extends AbstractCustomResourceTest<ClusterDto, StackGresCluster> {

  @Mock
  private CustomResourceFinder<ClusterResourceConsumtionDto> statusFinder;
  
  @Mock
  private CustomResourceFinder<ClusterPodConfigDto> detailsFinder;

  @Override
  protected CustomResourceList<StackGresCluster> getCustomResourceList() {
    return JsonUtil.readFromJson("stackgres_cluster/list.json", StackGresClusterList.class);
  }

  @Override
  protected ClusterDto getResourceDto() {
    return JsonUtil.readFromJson("stackgres_cluster/dto.json", ClusterDto.class);
  }

  @Override
  protected AbstractResourceTransformer<ClusterDto, StackGresCluster> getTransformer() {
    return new ClusterTransformer();
  }

  @Override
  protected AbstractRestService<ClusterDto, StackGresCluster> getService(
      CustomResourceScanner<StackGresCluster> scanner,
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      AbstractResourceTransformer<ClusterDto, StackGresCluster> transformer) {
    return new ClusterResource(scanner, finder, scheduler, transformer,
        statusFinder, detailsFinder);
  }

  @Override
  protected String getResourceNamespace() {
    return "postgresql";
  }

  @Override
  protected String getResourceName() {
    return "stackgres";
  }

  @Override
  protected void checkBackupConfig(ClusterDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("backupconf", resource.getSpec().getBackupConfig());
    assertEquals("pgbouncerconf", resource.getSpec().getConnectionPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getVolumeSize());
    assertEquals("standard", resource.getSpec().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getResourceProfile());
    assertNotNull(resource.getSpec().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004", resource.getSpec().getRestore().getBackupUid());
    assertIterableEquals(ImmutableList.of(
        "connection-pooling",
        "postgres-util",
        "prometheus-postgres-exporter"),
        resource.getSpec().getSidecars());
  }

  @Override
  protected void checkBackupConfig(StackGresCluster resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("postgresql", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertEquals("bfb53778-f59a-11e9-b1b5-0242ac110002", resource.getMetadata().getUid());
   assertNotNull(resource.getSpec());
    assertEquals("backupconf", resource.getSpec().getBackupConfig());
    assertEquals("pgbouncerconf", resource.getSpec().getConnectionPoolingConfig());
    assertEquals("5Gi", resource.getSpec().getVolumeSize());
    assertEquals("standard", resource.getSpec().getStorageClass());
    assertEquals(true, resource.getSpec().getPrometheusAutobind());
    assertEquals(1, resource.getSpec().getInstances());
    assertEquals("11.5", resource.getSpec().getPostgresVersion());
    assertEquals("postgresconf", resource.getSpec().getPostgresConfig());
    assertEquals("size-xs", resource.getSpec().getResourceProfile());
    assertNotNull(resource.getSpec().getRestore());
    assertEquals("d7e660a9-377c-11ea-b04b-0242ac110004", resource.getSpec().getRestore().getBackupUid());
    assertIterableEquals(ImmutableList.of(
        "connection-pooling",
        "postgres-util",
        "prometheus-postgres-exporter"),
        resource.getSpec().getSidecars());
  }

}