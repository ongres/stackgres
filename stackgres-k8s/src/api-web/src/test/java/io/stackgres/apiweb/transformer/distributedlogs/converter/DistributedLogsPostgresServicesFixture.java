/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs.converter;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;

import io.stackgres.apiweb.app.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.app.postgres.service.PostgresService;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPostgresServices;

public class DistributedLogsPostgresServicesFixture {

  DistributedLogsPostgresServices postgresServices = new DistributedLogsPostgresServices();

  public DistributedLogsPostgresServicesFixture withPrimary() {
    this.postgresServices.setPrimary(new EnabledPostgresService(CLUSTER_IP.toString()));
    return this;
  }

  public DistributedLogsPostgresServicesFixture withReplicas() {
    this.postgresServices.setReplicas(new PostgresService(true, CLUSTER_IP.toString()));
    return this;
  }

  public DistributedLogsPostgresServices build() {
    return this.postgresServices;
  }

}
