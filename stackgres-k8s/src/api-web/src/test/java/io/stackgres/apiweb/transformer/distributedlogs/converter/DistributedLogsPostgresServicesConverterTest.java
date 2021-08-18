/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.stackgres.apiweb.app.postgres.service.PostgresService;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPostgresServices;
import io.stackgres.apiweb.transformer.distributedlogs.DistributedLogsPostgresServicesConverter;
import io.stackgres.apiweb.transformer.distributedlogs.converter.fixture.StackGresDistributedLogsPostgresServicesFixture;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DistributedLogsPostgresServicesConverterTest {

  private DistributedLogsPostgresServicesConverter converter;

  @BeforeEach
  public void setup() {
    this.converter =  new DistributedLogsPostgresServicesConverter();
  }

  @Test
  void shouldPgServicePrimaryBeConvertedSuccessfully_fromSgDistributedLogsPgServicePrimary() {
    StackGresDistributedLogsPostgresServices sgPostgresServices =
        new StackGresDistributedLogsPostgresServicesFixture().withPrimary().build();
    DistributedLogsPostgresServices pgServicesConverted = converter.from(sgPostgresServices);
    StackGresPostgresService sgPgPrimary = sgPostgresServices.getPrimary();
    PostgresService dlPgPrimary = pgServicesConverted.getPrimary();

    assertEquals(sgPgPrimary.getEnabled(), dlPgPrimary.getEnabled());
    assertEquals(sgPgPrimary.getType(), dlPgPrimary.getType());
    assertEquals(sgPgPrimary.getAnnotations().size(), dlPgPrimary.getAnnotations().size());
    assertNull(pgServicesConverted.getReplicas());
  }

  @Test
  void shouldPgServiceReplicasBeConvertedSuccessfully_fromSgDistributedLogsPgServiceReplicas() {
    StackGresDistributedLogsPostgresServices sgPostgresServices =
        new StackGresDistributedLogsPostgresServicesFixture().withReplicas().build();
    DistributedLogsPostgresServices pgServicesConverted = converter.from(sgPostgresServices);
    StackGresPostgresService sgPgReplicas = sgPostgresServices.getReplicas();
    PostgresService dlPgReplicas = pgServicesConverted.getReplicas();

    assertEquals(sgPgReplicas.getEnabled(), dlPgReplicas.getEnabled());
    assertEquals(sgPgReplicas.getType(), dlPgReplicas.getType());
    assertEquals(sgPgReplicas.getAnnotations().size(), dlPgReplicas.getAnnotations().size());
    assertNull(pgServicesConverted.getPrimary());
  }

  @Test
  void shouldPgServicesPrimaryAndReplicasBeNull_onceThereIsNoSgDistributedLogsPgServices() {
    StackGresDistributedLogsPostgresServices sgPostgresServices =
        new StackGresDistributedLogsPostgresServicesFixture().build();
    DistributedLogsPostgresServices pgServicesConverted = converter.from(sgPostgresServices);
    assertNull(pgServicesConverted.getReplicas());
    assertNull(pgServicesConverted.getPrimary());
  }

  @Test
  void shouldPostgresServicesNull_onceSgDistributedLogsPostgresServicesWasInvalid() {
    DistributedLogsPostgresServices pgServicesConverted = converter.from(null);
    assertNull(pgServicesConverted);
  }
}
