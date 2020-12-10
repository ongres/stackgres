/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/stackgres/sgdbops")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DbOpsResource
    extends AbstractRestService<DbOpsDto, StackGresDbOps> {

  @Inject
  public DbOpsResource(
      CustomResourceScanner<StackGresDbOps> scanner,
      CustomResourceFinder<StackGresDbOps> finder,
      CustomResourceScheduler<StackGresDbOps> scheduler,
      ResourceTransformer<DbOpsDto, StackGresDbOps> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public DbOpsResource() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = DbOpsDto.class))) })
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public List<DbOpsDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DbOpsDto.class)) })
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public DbOpsDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void create(DbOpsDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void delete(DbOpsDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void update(DbOpsDto resource) {
    super.update(resource);
  }

}
