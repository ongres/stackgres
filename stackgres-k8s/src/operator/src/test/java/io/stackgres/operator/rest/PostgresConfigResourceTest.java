/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pgconfig.PostgresConfigDto;
import io.stackgres.operator.rest.transformer.AbstractResourceTransformer;
import io.stackgres.operator.rest.transformer.PostgresConfigTransformer;
import io.stackgres.operator.utils.JsonUtil;

import org.jooq.lambda.Seq;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresConfigResourceTest
    extends AbstractCustomResourceTest<PostgresConfigDto, StackGresPostgresConfig> {

  @Override
  protected CustomResourceList<StackGresPostgresConfig> getCustomResourceList() {
    return JsonUtil.readFromJson("postgres_config/list.json", StackGresPostgresConfigList.class);
  }

  @Override
  protected PostgresConfigDto getResourceDto() {
    return JsonUtil.readFromJson("postgres_config/dto.json", PostgresConfigDto.class);
  }

  @Override
  protected AbstractResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> getTransformer() {
    return new PostgresConfigTransformer();
  }

  @Override
  protected AbstractRestService<PostgresConfigDto, StackGresPostgresConfig> getService(
      CustomResourceScanner<StackGresPostgresConfig> scanner,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler,
      AbstractResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> transformer) {
    return new PostgresConfigResource(scanner, finder, scheduler, transformer);
  }

  @Override
  protected String getResourceNamespace() {
    return "default";
  }

  @Override
  protected String getResourceName() {
    return "postgresconf";
  }

  @Override
  protected void checkBackupConfig(PostgresConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("postgresconf", resource.getMetadata().getName());
    assertEquals("3658bd63-33cb-4948-8318-63183cbd2cf1", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("12", resource.getSpec().getPostgresVersion());
    assertEquals(Seq.of(
        "password_encryption='scram-sha-256'",
        "random_page_cost=1.5",
        "shared_buffers=256MB",
        "wal_compression=on")
        .toString("\n"),
        resource.getSpec().getPostgresqlConf());
  }

  @Override
  protected void checkBackupConfig(StackGresPostgresConfig resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("default", resource.getMetadata().getNamespace());
    assertEquals("postgresconf", resource.getMetadata().getName());
    assertEquals("3658bd63-33cb-4948-8318-63183cbd2cf1", resource.getMetadata().getUid());
    assertNotNull(resource.getSpec());
    assertEquals("12", resource.getSpec().getPostgresVersion());
    assertEquals(ImmutableMap.of(
        "password_encryption", "'scram-sha-256'",
        "random_page_cost", "1.5",
        "shared_buffers", "256MB",
        "wal_compression", "on"),
        resource.getSpec().getPostgresqlConf());
 }

}