/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.apiweb.distributedlogs.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

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
