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
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.ResourceScheduler;
import io.stackgres.operator.rest.dto.profile.ProfileDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

@Path("/stackgres/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource
    extends AbstractRestService<ProfileDto, StackGresProfile> {

  @Inject
  public ProfileResource(CustomResourceScanner<StackGresProfile> scanner,
      CustomResourceFinder<StackGresProfile> finder,
      ResourceScheduler<StackGresProfile> scheduler,
      ResourceTransformer<ProfileDto, StackGresProfile> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public ProfileResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
