/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@Path("sgshardedbackups")
@RequestScoped
@Authenticated
public class ShardedBackupResource
    extends AbstractCustomResourceService<ShardedBackupDto, StackGresShardedBackup> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ShardedBackupDto.class))) })
      })
  @Override
  public List<ShardedBackupDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ShardedBackupDto.class)) })
      })
  @Override
  public ShardedBackupDto create(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ShardedBackupDto.class)) })
      })
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
