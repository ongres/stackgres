/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.PostgresConfigResource;
import io.stackgres.apiweb.distributedlogs.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.transformer.AbstractDependencyResourceTransformer;
import io.stackgres.apiweb.transformer.PostgresConfigTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigList;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresConfigResourceTest
    extends AbstractDependencyCustomResourceTest<PostgresConfigDto, StackGresPostgresConfig,
      PostgresConfigResource> {

  @Override
  protected CustomResourceList<StackGresPostgresConfig> getCustomResourceList() {
    return JsonUtil.readFromJson("postgres_config/list.json", StackGresPostgresConfigList.class);
  }

  @Override
  protected PostgresConfigDto getResourceDto() {
    return JsonUtil.readFromJson("postgres_config/dto.json", PostgresConfigDto.class);
  }

  @Override
  protected AbstractDependencyResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> getTransformer() {
    return new PostgresConfigTransformer();
  }

  @Override
  protected PostgresConfigResource getService(
      CustomResourceScanner<StackGresPostgresConfig> scanner,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      AbstractDependencyResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> transformer) {
    return new PostgresConfigResource(scanner, finder, scheduler, clusterScanner, transformer);
  }

  @Override
  protected String getResourceNamespace() {
    return "stackgres";
  }

  @Override
  protected String getResourceName() {
    return "postgresconf";
  }

  @Override
  protected void checkDto(PostgresConfigDto resource) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
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
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(1, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
  }

  @Override
  protected void checkCustomResource(StackGresPostgresConfig resource, Operation operation) {
    assertNotNull(resource.getMetadata());
    assertEquals("stackgres", resource.getMetadata().getNamespace());
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