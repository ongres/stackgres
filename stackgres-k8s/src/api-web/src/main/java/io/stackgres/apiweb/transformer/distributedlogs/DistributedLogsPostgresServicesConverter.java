/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.distributedlogs;

import io.stackgres.apiweb.app.postgres.service.EnabledPostgresService;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsPostgresServices;
import io.stackgres.apiweb.transformer.converter.DtoConverter;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPostgresServices;

public class DistributedLogsPostgresServicesConverter implements
    DtoConverter<StackGresDistributedLogsPostgresServices, DistributedLogsPostgresServices> {

  @Override
  public DistributedLogsPostgresServices from(StackGresDistributedLogsPostgresServices source) {
    if (source == null) {
      return null;
    }

    return new DistributedLogsPostgresServices(
        convertToPostgreService(source.getPrimary()),
        convertToPostgreService(source.getReplicas()));
  }

  private EnabledPostgresService convertToPostgreService(
      StackGresPostgresService sgPostgresService) {
    EnabledPostgresService postgresService = null;

    if (sgPostgresService != null) {
      postgresService =
          new EnabledPostgresService(
              sgPostgresService.getType(),
              sgPostgresService.getAnnotations());
    }
    return postgresService;
  }
}
