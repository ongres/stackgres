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
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;

@Path("/stackgres/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresProfileResource
    extends AbstractCustomResourceRestService<StackGresProfile> {

  @Inject
  public StackGresProfileResource(KubernetesCustomResourceScanner<StackGresProfile> scanner,
      KubernetesCustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(scanner, finder, scheduler);
  }

  public StackGresProfileResource() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
