/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.stackgres.apiweb.dto.fixture.DtoFixtures;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.rest.sgpoolconfig.ConnectionPoolingConfigResource;
import io.stackgres.apiweb.rest.sgpoolconfig.NamespacedConnectionPoolingConfigResource;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.PoolingConfigTransformer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.fixture.poolingconfig.PoolingConfigListFixture;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgbouncerConfigResourceTest extends AbstractDependencyCustomResourceTest
      <PoolingConfigDto, StackGresPoolingConfig,
      ConnectionPoolingConfigResource, NamespacedConnectionPoolingConfigResource> {

  @Override
  protected DefaultKubernetesResourceList<StackGresPoolingConfig> getCustomResourceList() {
    return PoolingConfigListFixture.fixture().loadDefault().get();
  }

  @Override
  protected PoolingConfigDto getResourceDto() {
    return DtoFixtures.poolingConfig().loadDefault().get();
  }

  @Override
  protected AbstractDependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig>
      getTransformer() {
    var mapper = JsonMapper.builder().build();
    return new PoolingConfigTransformer(mapper);
  }

  @Override
  protected ConnectionPoolingConfigResource getService() {
    return new ConnectionPoolingConfigResource();
  }

  @Override
  protected NamespacedConnectionPoolingConfigResource getNamespacedService() {
    return new NamespacedConnectionPoolingConfigResource();
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "pgbouncerconf";
  }

  @Override
  protected void checkDto(PoolingConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertEquals("ceaa793f-2d97-48b7-91e4-8086b22f1c4c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("""
        [databases]
        foodb = dbname=bardb pool_size=10
        sgdb = pool_mode=statement max_db_connections=1000

        [users]
        user1 = max_user_connections=30
        user2 = pool_mode=session max_user_connections=100

        [pgbouncer]
        default_pool_size = 200
        max_client_conn = 100
        pool_mode = 'transaction'

        """,
        resource.getSpec().getPgBouncer().getParameters());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(2, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
    assertNotNull(resource.getStatus().getPgBouncer());
    assertNotNull(resource.getStatus().getPgBouncer().getPgbouncerIni());
    assertEquals(3, resource.getStatus().getPgBouncer().getPgbouncerIni().size());
    assertEquals("default_pool_size",
        resource.getStatus().getPgBouncer().getPgbouncerIni().get(0).getParameter());
    assertEquals("200", resource.getStatus().getPgBouncer().getPgbouncerIni().get(0).getValue());
    assertEquals("max_client_conn",
        resource.getStatus().getPgBouncer().getPgbouncerIni().get(1).getParameter());
    assertEquals("100", resource.getStatus().getPgBouncer().getPgbouncerIni().get(1).getValue());
    assertEquals("pool_mode",
        resource.getStatus().getPgBouncer().getPgbouncerIni().get(2).getParameter());
    assertEquals("'transaction'",
        resource.getStatus().getPgBouncer().getPgbouncerIni().get(2).getValue());
    assertNotNull(resource.getStatus().getPgBouncer().getDefaultParameters());
    assertIterableEquals(ImmutableSet.of(
        "default_pool_size",
        "max_client_conn",
        "pool_mode"),
        resource.getStatus().getPgBouncer().getDefaultParameters().keySet());
  }

  @Override
  protected void checkCustomResource(StackGresPoolingConfig resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertEquals("ceaa793f-2d97-48b7-91e4-8086b22f1c4c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals(ImmutableMap.of(
        "default_pool_size", "200",
        "max_client_conn", "100",
        "pool_mode", "'transaction'"),
        resource.getSpec().getPgBouncer().getPgbouncerIni().getPgbouncer());
  }

}
