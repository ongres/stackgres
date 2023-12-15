/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgshardeddbops;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgshardeddbops")
@RequestScoped
@Authenticated
@Tag(name = "sgshardeddbops")
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
public class ShardedDbOpsResource
    extends AbstractCustomResourceService<ShardedDbOpsDto, StackGresShardedDbOps> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ShardedDbOpsDto.class))})
  @Operation(summary = "List sgshardeddbops", description = """
      List sgshardeddbops.

      ### RBAC permissions required

      * sgshardeddbops list
      """)
  @Override
  public List<ShardedDbOpsDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedDbOpsDto.class))})
  @Operation(summary = "Create a sgshardeddbops", description = """
      Create a sgshardeddbops.

      ### RBAC permissions required

      * sgshardeddbops create
      """)
  @Override
  public ShardedDbOpsDto create(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgshardeddbops", description = """
      Delete a sgshardeddbops.

      ### RBAC permissions required

      * sgshardeddbops delete
      """)
  @Override
  public void delete(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedDbOpsDto.class))})
  @Operation(summary = "Update a sgshardeddbops", description = """
      Update a sgshardeddbops.

      ### RBAC permissions required

      * sgshardeddbops patch
      """)
  @Override
  public ShardedDbOpsDto update(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(
      StackGresShardedDbOps resourceToUpdate, StackGresShardedDbOps resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
