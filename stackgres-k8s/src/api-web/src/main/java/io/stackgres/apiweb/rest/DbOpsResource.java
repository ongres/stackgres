/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("")
@RequestScoped
@Authenticated
public class DbOpsResource
    extends AbstractRestService<DbOpsDto, StackGresDbOps> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = DbOpsDto.class))) })
      })
  @Override
  @Path("sgdbops")
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
  @Override
  @Path("{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgdbops/{name}")
  public DbOpsDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgdbops")
  public void create(DbOpsDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgdbops")
  public void delete(DbOpsDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgdbops")
  public void update(DbOpsDto resource) {
    super.update(resource);
  }

}
