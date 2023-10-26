/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("sgshardeddbops")
@RequestScoped
@Authenticated
public class ShardedDbOpsResource
    extends AbstractCustomResourceService<ShardedDbOpsDto, StackGresShardedDbOps> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ShardedDbOpsDto.class))) })
      })
  @Override
  public List<ShardedDbOpsDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ShardedDbOpsDto.class)) })
      })
  @Override
  public ShardedDbOpsDto create(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ShardedDbOpsDto.class)) })
      })
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
