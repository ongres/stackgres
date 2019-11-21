/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;

@Path("/stackgres/pgconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresPostgresConfigResource extends
    AbstractCustomResourceRestService<StackGresPostgresConfig, StackGresPostgresConfigList> {

  public StackGresPostgresConfigResource() {
    super(StackGresPostgresConfigDefinition.NAME);
  }

}
