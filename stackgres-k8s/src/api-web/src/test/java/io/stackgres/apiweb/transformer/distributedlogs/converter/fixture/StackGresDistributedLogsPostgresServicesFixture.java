/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs.converter.fixture;

import java.util.HashMap;

import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;

public class StackGresDistributedLogsPostgresServicesFixture {

  StackGresDistributedLogsPostgresServices postgresServices =
      new StackGresDistributedLogsPostgresServices();

  public StackGresDistributedLogsPostgresServicesFixture withPrimary() {

    StackGresPostgresService primary = new StackGresPostgresService();
    primary.setEnabled(true);
    primary.setType(StackGresPostgresServiceType.CLUSTER_IP.name());
    primary.setAnnotations(new HashMap<String, String>());
    postgresServices.setPrimary(primary);

    return this;
  }

  public StackGresDistributedLogsPostgresServicesFixture withReplicas() {

    StackGresPostgresService replicas = new StackGresPostgresService();
    replicas.setEnabled(true);
    replicas.setType(StackGresPostgresServiceType.CLUSTER_IP.name());
    replicas.setAnnotations(new HashMap<String, String>());
    this.postgresServices.setReplicas(replicas);

    return this;
  }

  public StackGresDistributedLogsPostgresServices build() {
    return this.postgresServices;
  }

}
