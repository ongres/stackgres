/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pooling.PoolingConfigDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.PoolingConfigTransformer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigList;
import io.stackgres.operator.utils.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PgbouncerConfigResourceTest
    extends AbstractCustomResourceTest<PoolingConfigDto, StackGresPoolingConfig> {

  @Override
  protected CustomResourceList<StackGresPoolingConfig> getCustomResourceList() {
    return JsonUtil.readFromJson("pooling_config/list.json", StackGresPoolingConfigList.class);
  }

  @Override
  protected PoolingConfigDto getResourceDto() {
    return JsonUtil.readFromJson("pooling_config/dto.json", PoolingConfigDto.class);
  }

  @Override
  protected AbstractResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> getTransformer() {
    return new PoolingConfigTransformer();
  }

  @Override
  protected AbstractRestService<PoolingConfigDto, StackGresPoolingConfig> getService(
      CustomResourceScanner<StackGresPoolingConfig> scanner,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler,
      AbstractResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> transformer) {
    return new ConnectionPoolingConfigResource(scanner, finder, scheduler, transformer);
  }

  @Override
  protected String getResourceNamespace() {
    return "default";
  }

  @Override
  protected String getResourceName() {
    return "pgbouncerconf";
  }

  @Override
  protected void checkBackupConfig(PoolingConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertEquals("ceaa793f-2d97-48b7-91e4-8086b22f1c4c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals(Seq.of(
        "default_pool_size=200", 
        "max_client_conn=200",
        "pool_mode='transaction'")
        .toString("\n"),
        resource.getSpec().getPgBouncer().getPgbouncerConf());
  }

  @Override
  protected void checkBackupConfig(StackGresPoolingConfig resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertEquals("ceaa793f-2d97-48b7-91e4-8086b22f1c4c", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals(ImmutableMap.of(
        "default_pool_size", "200", 
        "max_client_conn", "200",
        "pool_mode", "'transaction'"),
        resource.getSpec().getPgBouncer().getPgbouncerConf());
  }

}