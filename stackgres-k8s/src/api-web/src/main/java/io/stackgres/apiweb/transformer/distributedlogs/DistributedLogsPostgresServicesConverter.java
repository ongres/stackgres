/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs;

import java.util.Optional;

import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPostgresServices;
import io.stackgres.apiweb.dto.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.dto.postgres.service.PostgresService;
import io.stackgres.apiweb.transformer.converter.DtoConverter;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServicesBuilder;

public class DistributedLogsPostgresServicesConverter implements
    DtoConverter<StackGresDistributedLogsPostgresServices, DistributedLogsPostgresServices> {

  @Override
  public DistributedLogsPostgresServices from(StackGresDistributedLogsPostgresServices source) {
    return Optional.ofNullable(source)
        .map(sgDlPgServices -> {
          return new DistributedLogsPostgresServices(
              convertToEnabledPostgreService(sgDlPgServices.getPrimary()),
              convertToPostgreService(sgDlPgServices.getReplicas()));
        }).orElse(null);
  }

  private PostgresService convertToPostgreService(StackGresPostgresService replicas) {
    return Optional.ofNullable(replicas)
        .map(pgReplicas -> {
          var postgresService = new PostgresService();
          postgresService.setEnabled(pgReplicas.getEnabled());
          postgresService.setType(pgReplicas.getType());
          postgresService.setLoadBalancerIP(pgReplicas.getLoadBalancerIP());
          postgresService.setExternalIPs(pgReplicas.getExternalIPs());
          return postgresService;
        }).orElse(null);
  }

  private EnabledPostgresService convertToEnabledPostgreService(
      StackGresPostgresService sgPostgresService) {
    return Optional.ofNullable(sgPostgresService)
        .map(sgPgServices -> {
          var enabledPostgresService = new EnabledPostgresService();
          enabledPostgresService.setType(sgPgServices.getType());
          enabledPostgresService.setExternalIPs(sgPgServices.getExternalIPs());
          enabledPostgresService.setLoadBalancerIP(sgPgServices.getLoadBalancerIP());
          return enabledPostgresService;
        }).orElse(null);
  }

  public StackGresDistributedLogsPostgresServices to(
      DistributedLogsPostgresServices postgresServices) {
    return Optional.ofNullable(postgresServices)
        .map(pgServices -> new StackGresDistributedLogsPostgresServicesBuilder()
            .withPrimary(map(pgServices.getPrimary()))
            .withReplicas(map(pgServices.getReplicas()))
            .build()).orElse(null);
  }

  private StackGresPostgresService map(PostgresService postgresService) {
    return Optional.ofNullable(postgresService)
        .map(pgServices -> {
          return new StackGresPostgresService(pgServices.getEnabled(),
              pgServices.getType(), pgServices.getExternalIPs(), pgServices.getLoadBalancerIP());
        }).orElse(null);

  }
}
