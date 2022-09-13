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
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresDistributedLogsUtilTest {

  private StackGresPostgresService primary;
  private StackGresPostgresService replicas;

  @BeforeEach
  public void setup() {
    primary = Fixtures.cluster().loadDefault().get().getSpec().getPostgresServices().getPrimary();
    primary.setEnabled(false);
    primary.setType(EXTERNAL_NAME.toString());
    replicas = Fixtures.cluster().loadDefault().get().getSpec().getPostgresServices().getReplicas();
    primary.setEnabled(true);
    primary.setType(CLUSTER_IP.toString());
  }

  @Test
  void shouldBuildSgDistributedLogsPostgresServices_asEmptyAvoidingAnyException() {
    assertNotNull(StackGresDistributedLogsUtil
        .buildClusterPostgresServices(
            Fixtures.distributedLogs().spec().emptyPostgresServices().get()));
  }

  @Test
  void shouldBuildSgDistributedLogsPostgresServices_withOnlyPrimary() {

    StackGresDistributedLogsSpec spec = Fixtures.distributedLogs().spec()
        .withPrimaryPostgresServices(primary).get();
    StackGresClusterPostgresServices postgresServices = StackGresDistributedLogsUtil
        .buildClusterPostgresServices(spec);

    assertNotNull(postgresServices.getPrimary());
    assertNull(postgresServices.getReplicas());
    assertEquals(primary, postgresServices.getPrimary());
  }

  @Test
  void shouldBuildSgDistributedLogsPostgresServices_withOnlyReplicas() {

    StackGresDistributedLogsSpec spec = Fixtures.distributedLogs().spec()
        .withReplicasPostgresServices(replicas).get();
    StackGresClusterPostgresServices postgresServices = StackGresDistributedLogsUtil
        .buildClusterPostgresServices(spec);

    assertNull(postgresServices.getPrimary());
    assertNotNull(postgresServices.getReplicas());
    assertEquals(replicas, postgresServices.getReplicas());
  }

}
