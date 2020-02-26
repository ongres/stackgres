/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.PgbouncerConfigTransformer;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import io.stackgres.operator.utils.JsonUtil;

import org.jooq.lambda.Seq;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgbouncerConfigResourceTest
    extends AbstractCustomResourceTest<PgbouncerConfigDto, StackGresPgbouncerConfig> {

  @Override
  protected CustomResourceList<StackGresPgbouncerConfig> getCustomResourceList() {
    return JsonUtil.readFromJson("pgbouncer_config/list.json", StackGresPgbouncerConfigList.class);
  }

  @Override
  protected PgbouncerConfigDto getResourceDto() {
    return JsonUtil.readFromJson("pgbouncer_config/dto.json", PgbouncerConfigDto.class);
  }

  @Override
  protected AbstractResourceTransformer<PgbouncerConfigDto, StackGresPgbouncerConfig> getTransformer() {
    return new PgbouncerConfigTransformer();
  }

  @Override
  protected AbstractRestService<PgbouncerConfigDto, StackGresPgbouncerConfig> getService(
      CustomResourceScanner<StackGresPgbouncerConfig> scanner,
      CustomResourceFinder<StackGresPgbouncerConfig> finder,
      CustomResourceScheduler<StackGresPgbouncerConfig> scheduler,
      AbstractResourceTransformer<PgbouncerConfigDto, StackGresPgbouncerConfig> transformer) {
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
  protected void checkBackupConfig(PgbouncerConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertNotNull(resource.getSpec());
    assertEquals(Seq.of(
        "default_pool_size=200", 
        "max_client_conn=200",
        "pool_mode='transaction'")
        .toString("\n"),
        resource.getSpec().getPgbouncerConf());
  }

  @Override
  protected void checkBackupConfig(StackGresPgbouncerConfig resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("pgbouncerconf", resource.getMetadata().getName());
    assertNotNull(resource.getSpec());
    assertEquals(ImmutableMap.of(
        "default_pool_size", "200", 
        "max_client_conn", "200",
        "pool_mode", "'transaction'"),
        resource.getSpec().getPgbouncerConf());
  }

}