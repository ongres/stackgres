/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;

@Path("/stackgres/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresProfileResource
    extends AbstractCustomResourceRestService<StackGresProfile, StackGresProfileList> {

  public StackGresProfileResource() {

    super(StackGresProfileDefinition.NAME);

  }

}
