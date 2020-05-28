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

import io.stackgres.apiweb.distributedlogs.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

@Path("/stackgres/sgpoolconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionPoolingConfigResource extends
    AbstractRestService<PoolingConfigDto, StackGresPoolingConfig> {

  @Inject
  public ConnectionPoolingConfigResource(
      CustomResourceScanner<StackGresPoolingConfig> scanner,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler,
      ResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public ConnectionPoolingConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
