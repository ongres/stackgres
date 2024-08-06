/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.apiweb.rest.sgshardeddbops.NamespacedShardedDbOpsResource;
import io.stackgres.apiweb.rest.sgshardeddbops.ShardedDbOpsResource;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.ShardedDbOpsTransformer;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsResourceTest extends AbstractCustomResourceTest
      <ShardedDbOpsDto, StackGresShardedDbOps,
      ShardedDbOpsResource, NamespacedShardedDbOpsResource> {

  @Override
  protected DefaultKubernetesResourceList<StackGresShardedDbOps> getCustomResourceList() {
    return Fixtures.shardedDbOpsList().loadDefault().withJustFirstElement().get();
  }

  @Override
  protected ShardedDbOpsDto getDto() {
    return DtoFixtures.shardedDbOps().loadDefault().get();
  }

  @Override
  protected AbstractResourceTransformer<ShardedDbOpsDto, StackGresShardedDbOps> getTransformer() {
    final JsonMapper mapper = JsonMapper.builder().build();
    return new ShardedDbOpsTransformer(mapper);
  }

  @Override
  protected ShardedDbOpsResource getService() {
    return new ShardedDbOpsResource();
  }

  @Override
  protected NamespacedShardedDbOpsResource getNamespacedService() {
    return new NamespacedShardedDbOpsResource();
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
  protected void checkDto(ShardedDbOpsDto dto, StackGresShardedDbOps resource) {
    assertNotNull(dto.getMetadata());
    assertEquals("default", dto.getMetadata().getNamespace());
    assertEquals("stackgres", dto.getMetadata().getName());
    assertNotNull(dto.getSpec());
    assertEquals("test", dto.getSpec().getSgShardedCluster());
  }

  @Override
  protected void checkCustomResource(StackGresShardedDbOps resource, ShardedDbOpsDto resourceDto,
      Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertNotNull(resource.getSpec());
    assertEquals("test", resource.getSpec().getSgShardedCluster());
  }

}
