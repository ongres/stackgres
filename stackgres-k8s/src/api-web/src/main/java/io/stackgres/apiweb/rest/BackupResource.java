/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("")
@RequestScoped
@Authenticated
public class BackupResource
    extends AbstractRestService<BackupDto, StackGresBackup> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = BackupDto.class))) })
      })
  @Override
  @Path("sgbackups")
  public List<BackupDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BackupDto.class)) })
      })
  @Override
  @Path("{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgbackups/{name}")
  public BackupDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgbackups")
  public void create(BackupDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgbackups")
  public void delete(BackupDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgbackups")
  public void update(BackupDto resource) {
    super.update(resource);
  }

}
