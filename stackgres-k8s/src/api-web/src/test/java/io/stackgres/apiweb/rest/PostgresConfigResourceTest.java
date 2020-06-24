/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
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
        "max_wal_senders=10",
        "pg_stat_statements.max=10000")
        .toString("\n"),
        resource.getSpec().getPostgresqlConf());
    assertNotNull(resource.getStatus());
    assertNotNull(resource.getStatus().getClusters());
    assertEquals(1, resource.getStatus().getClusters().size());
    assertEquals("stackgres", resource.getStatus().getClusters().get(0));
    assertNotNull(resource.getStatus().getPostgresqlConf());
    assertEquals(5, resource.getStatus().getPostgresqlConf().size());
    assertEquals("password_encryption", resource.getStatus().getPostgresqlConf().get(0).getParameter());
    assertEquals("'scram-sha-256'", resource.getStatus().getPostgresqlConf().get(0).getValue());
    assertEquals("https://postgresqlco.nf/en/doc/param/password_encryption/12/",
        resource.getStatus().getPostgresqlConf().get(0).getDocumentationLink());
    assertEquals("random_page_cost", resource.getStatus().getPostgresqlConf().get(1).getParameter());
    assertEquals("1.5", resource.getStatus().getPostgresqlConf().get(1).getValue());
    assertEquals("https://postgresqlco.nf/en/doc/param/random_page_cost/12/",
        resource.getStatus().getPostgresqlConf().get(1).getDocumentationLink());
    assertEquals("shared_buffers", resource.getStatus().getPostgresqlConf().get(2).getParameter());
    assertEquals("256MB", resource.getStatus().getPostgresqlConf().get(2).getValue());
    assertEquals("https://postgresqlco.nf/en/doc/param/shared_buffers/12/",
        resource.getStatus().getPostgresqlConf().get(2).getDocumentationLink());
    assertEquals("max_wal_senders", resource.getStatus().getPostgresqlConf().get(3).getParameter());
    assertEquals("10", resource.getStatus().getPostgresqlConf().get(3).getValue());
    assertEquals("https://postgresqlco.nf/en/doc/param/max_wal_senders/12/",
        resource.getStatus().getPostgresqlConf().get(3).getDocumentationLink());
    assertEquals("pg_stat_statements.max", resource.getStatus().getPostgresqlConf().get(4).getParameter());
    assertEquals("10000", resource.getStatus().getPostgresqlConf().get(4).getValue());
    assertNull(resource.getStatus().getPostgresqlConf().get(4).getDocumentationLink());
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
        "max_wal_senders", "10",
        "pg_stat_statements.max", "10000"),
        resource.getSpec().getPostgresqlConf());
 }

}