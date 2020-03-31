/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pgconfig.PostgresConfigDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

@Path("/stackgres/sgpgconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostgresConfigResource extends
    AbstractRestService<PostgresConfigDto, StackGresPostgresConfig> {

  @Inject
  public PostgresConfigResource(
      CustomResourceScanner<StackGresPostgresConfig> scanner,
      CustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler,
      ResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public PostgresConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
