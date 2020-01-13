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
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;

@Path("/stackgres/pgconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresPostgresConfigResource extends
    AbstractCustomResourceRestService<StackGresPostgresConfig> {

  @Inject
  public StackGresPostgresConfigResource(
      KubernetesCustomResourceScanner<StackGresPostgresConfig> scanner,
      KubernetesCustomResourceFinder<StackGresPostgresConfig> finder,
      CustomResourceScheduler<StackGresPostgresConfig> scheduler) {
    super(scanner, finder, scheduler);
  }

  public StackGresPostgresConfigResource() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
