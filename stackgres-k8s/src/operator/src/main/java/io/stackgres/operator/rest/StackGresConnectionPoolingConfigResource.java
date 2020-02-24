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
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;

@Path("/stackgres/connpoolconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresConnectionPoolingConfigResource extends
    AbstractCustomResourceRestService<PgbouncerConfigDto, StackGresPgbouncerConfig> {

  @Inject
  public StackGresConnectionPoolingConfigResource(
      CustomResourceScanner<StackGresPgbouncerConfig> scanner,
      CustomResourceFinder<StackGresPgbouncerConfig> finder,
      CustomResourceScheduler<StackGresPgbouncerConfig> scheduler,
      ResourceTransformer<PgbouncerConfigDto, StackGresPgbouncerConfig> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public StackGresConnectionPoolingConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
