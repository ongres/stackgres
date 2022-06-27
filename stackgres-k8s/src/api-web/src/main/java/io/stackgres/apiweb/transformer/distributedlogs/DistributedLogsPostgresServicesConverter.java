/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs;

import java.util.Optional;

import io.stackgres.apiweb.app.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.app.postgres.service.PostgresService;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPostgresServices;
import io.stackgres.apiweb.transformer.converter.DtoConverter;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;

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
          return new PostgresService(pgReplicas.getEnabled(), pgReplicas.getType());
        }).orElse(null);
  }

  private EnabledPostgresService convertToEnabledPostgreService(
      StackGresPostgresService sgPostgresService) {
    return Optional.ofNullable(sgPostgresService)
        .map(sgPgServices -> {
          return new EnabledPostgresService(sgPgServices.getType());
        }).orElse(null);
  }

  public StackGresDistributedLogsPostgresServices to(
      DistributedLogsPostgresServices postgresServices) {
    return Optional.ofNullable(postgresServices)
        .map(pgServices -> {
          return new StackGresDistributedLogsPostgresServices(map(pgServices.getPrimary()),
              map(pgServices.getReplicas()));
        }).orElse(null);

  }

  private StackGresPostgresService map(PostgresService postgresService) {
    return Optional.ofNullable(postgresService)
        .map(pgServices -> {
          return new StackGresPostgresService(pgServices.getEnabled(),
              pgServices.getType());
        }).orElse(null);

  }
}
