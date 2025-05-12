/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgdbops;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgdbops")
@RequestScoped
@Authenticated
@Tag(name = "sgdbops")
@APIResponse(responseCode = "400", description = "Bad Request",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "401", description = "Unauthorized",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "403", description = "Forbidden",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "500", description = "Internal Server Error",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
public class DbOpsResource
    extends AbstractCustomResourceService<DbOpsDto, StackGresDbOps> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(type = SchemaType.ARRAY, implementation = DbOpsDto.class))})
  @Operation(summary = "List sgdbops", description = """
      List sgdbops.

      ### RBAC permissions required

      * sgdbops list
      """)
  @Override
  public List<DbOpsDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DbOpsDto.class))})
  @Operation(summary = "Create a sgdbops", description = """
      Create a sgdbops.

      ### RBAC permissions required

      * sgdbops create
      """)
  @Override
  public DbOpsDto create(DbOpsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgdbops", description = """
      Delete a sgdbops.

      ### RBAC permissions required

      * sgdbops delete
      """)
  @Override
  public void delete(DbOpsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DbOpsDto.class))})
  @Operation(summary = "Update a sgdbops", description = """
      Update a sgdbops.

      ### RBAC permissions required

      * sgdbops patch
      """)
  @Override
  public DbOpsDto update(DbOpsDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresDbOps resourceToUpdate, StackGresDbOps resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
