/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs.converter;

import static io.stackgres.common.crd.postgres.service.StackGresPostgresServiceType.CLUSTER_IP;

import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPostgresServices;
import io.stackgres.apiweb.dto.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.dto.postgres.service.PostgresService;

public class DistributedLogsPostgresServicesFixture {

  DistributedLogsPostgresServices postgresServices = new DistributedLogsPostgresServices();

  public DistributedLogsPostgresServicesFixture withPrimary() {
    var enabledPostgresService = new EnabledPostgresService();
    enabledPostgresService.setType(CLUSTER_IP.toString());
    this.postgresServices.setPrimary(enabledPostgresService);
    return this;
  }

  public DistributedLogsPostgresServicesFixture withReplicas() {
    var postgresService = new PostgresService();
    postgresService.setType(CLUSTER_IP.toString());
    this.postgresServices.setReplicas(postgresService);
    return this;
  }

  public DistributedLogsPostgresServices build() {
    return this.postgresServices;
  }

}
