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

import io.stackgres.apiweb.distributedlogs.dto.profile.ProfileDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

@Path("/stackgres/sginstanceprofile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource
    extends AbstractRestService<ProfileDto, StackGresProfile> {

  @Inject
  public ProfileResource(CustomResourceScanner<StackGresProfile> scanner,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler,
      ResourceTransformer<ProfileDto, StackGresProfile> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public ProfileResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
