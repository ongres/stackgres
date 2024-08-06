/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.rest.sgdbops.DbOpsResource;
import io.stackgres.apiweb.rest.sgdbops.NamespacedDbOpsResource;
import io.stackgres.apiweb.transformer.AbstractResourceTransformer;
import io.stackgres.apiweb.transformer.DbOpsTransformer;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsResourceTest extends AbstractCustomResourceTest
      <DbOpsDto, StackGresDbOps, DbOpsResource, NamespacedDbOpsResource> {

  @Override
  protected DefaultKubernetesResourceList<StackGresDbOps> getCustomResourceList() {
    return Fixtures.dbOpsList().loadDefault().withJustFirstElement().get();
  }

  @Override
  protected DbOpsDto getDto() {
    return DtoFixtures.dbOps().loadDefault().get();
  }

  @Override
  protected AbstractResourceTransformer<DbOpsDto, StackGresDbOps> getTransformer() {
    final JsonMapper mapper = JsonMapper.builder().build();
    return new DbOpsTransformer(mapper);
  }

  @Override
  protected DbOpsResource getService() {
    return new DbOpsResource();
  }

  @Override
  protected NamespacedDbOpsResource getNamespacedService() {
    return new NamespacedDbOpsResource();
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
  protected void checkDto(DbOpsDto dto, StackGresDbOps resource) {
    assertNotNull(dto.getMetadata());
    assertEquals("default", dto.getMetadata().getNamespace());
    assertEquals("stackgres", dto.getMetadata().getName());
    assertNotNull(dto.getSpec());
    assertEquals("test", dto.getSpec().getSgCluster());
  }

  @Override
  protected void checkCustomResource(StackGresDbOps resource, DbOpsDto resourceDto,
      Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("stackgres", resource.getMetadata().getName());
    assertNotNull(resource.getSpec());
    assertEquals("test", resource.getSpec().getSgCluster());
  }

}
