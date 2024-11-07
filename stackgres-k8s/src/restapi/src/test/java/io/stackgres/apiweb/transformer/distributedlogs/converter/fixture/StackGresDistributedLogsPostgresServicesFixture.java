/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs.converter.fixture;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;

public class StackGresDistributedLogsPostgresServicesFixture {

  StackGresPostgresServices postgresServices =
      new StackGresPostgresServices();

  public StackGresDistributedLogsPostgresServicesFixture withPrimary() {

    StackGresPostgresService primary = new StackGresPostgresService();
    primary.setEnabled(true);
    primary.setType(StackGresPostgresServiceType.CLUSTER_IP.name());
    postgresServices.setPrimary(primary);

    return this;
  }

  public StackGresDistributedLogsPostgresServicesFixture withReplicas() {

    StackGresPostgresService replicas = new StackGresPostgresService();
    replicas.setEnabled(true);
    replicas.setType(StackGresPostgresServiceType.CLUSTER_IP.name());
    this.postgresServices.setReplicas(replicas);

    return this;
  }

  public StackGresPostgresServices build() {
    return this.postgresServices;
  }

}
