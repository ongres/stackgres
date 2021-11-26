/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;
import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.EXTERNAL_NAME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.fixture.StackGresDistributedLogsSpecFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresDistributedLogsUtilTest {

  private StackGresPostgresService primary;
  private StackGresPostgresService replicas;

  @BeforeEach
  public void setup() {
    this.primary = new StackGresPostgresServiceFixture().isEnabled(false).withType(EXTERNAL_NAME)
        .build();
    this.replicas = new StackGresPostgresServiceFixture().isEnabled(true).withType(CLUSTER_IP)
        .build();
  }

  @Test
  void shouldBuildSgDistributedLogsPostgresServices_asEmptyAvoidingAnyException() {
    assertNotNull(StackGresDistributedLogsUtil
        .buildPostgresServices(new StackGresDistributedLogsSpecFixture().build()));
  }

  @Test
  void shouldBuildSgDistributedLogsPostgresServices_withOnlyPrimary() {

    StackGresDistributedLogsSpec spec = new StackGresDistributedLogsSpecFixture()
        .withPrimaryPostgresServices(primary).build();
    StackGresClusterPostgresServices postgresServices = StackGresDistributedLogsUtil
        .buildPostgresServices(spec);

    assertNotNull(postgresServices.getPrimary());
    assertNull(postgresServices.getReplicas());
    assertEquals(primary, postgresServices.getPrimary());
  }

  @Test
  void shouldBuildSgDistributedLogsPostgresServices_withOnlyReplicas() {

    StackGresDistributedLogsSpec spec = new StackGresDistributedLogsSpecFixture()
        .withReplicasPostgresServices(replicas).build();
    StackGresClusterPostgresServices postgresServices = StackGresDistributedLogsUtil
        .buildPostgresServices(spec);

    assertNull(postgresServices.getPrimary());
    assertNotNull(postgresServices.getReplicas());
    assertEquals(replicas, postgresServices.getReplicas());
  }

}
