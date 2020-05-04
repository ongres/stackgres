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

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

@Path("/stackgres/sgdistributedlogs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DistributedLogsResource
    extends AbstractRestService<DistributedLogsDto, StackGresDistributedLogs> {

  @Inject
  public DistributedLogsResource(CustomResourceScanner<StackGresDistributedLogs> scanner,
      CustomResourceFinder<StackGresDistributedLogs> finder,
      CustomResourceScheduler<StackGresDistributedLogs> scheduler,
      ResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public DistributedLogsResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
