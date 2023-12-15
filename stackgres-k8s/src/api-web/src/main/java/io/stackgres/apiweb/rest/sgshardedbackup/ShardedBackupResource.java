/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgshardedbackup;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgshardedbackups")
@RequestScoped
@Authenticated
@Tag(name = "sgshardedbackup")
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
public class ShardedBackupResource
    extends AbstractCustomResourceService<ShardedBackupDto, StackGresShardedBackup> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ShardedBackupDto.class))})
  @Operation(summary = "List sgshardedbackups", description = """
      List sgshardedbackups.

      ### RBAC permissions required

      * sgshardedbackups list
      """)
  @Override
  public List<ShardedBackupDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedBackupDto.class))})
  @Operation(summary = "Create a sgshardedbackup", description = """
      Create a sgshardedbackup.

      ### RBAC permissions required

      * sgshardedbackup create
      """)
  @Override
  public ShardedBackupDto create(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sgshardedbackup", description = """
      Delete a sgshardedbackup.

      ### RBAC permissions required

      * sgshardedbackup delete
      """)
  @Override
  public void delete(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedBackupDto.class))})
  @Operation(summary = "Update a sgshardedbackup", description = """
      Update a sgshardedbackup.

      ### RBAC permissions required

      * sgshardedbackup patch
      """)
  @Override
  public ShardedBackupDto update(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(
      StackGresShardedBackup resourceToUpdate, StackGresShardedBackup resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
