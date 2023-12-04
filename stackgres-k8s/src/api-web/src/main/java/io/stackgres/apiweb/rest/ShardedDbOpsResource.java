/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("sgshardeddbops")
@RequestScoped
@Authenticated
public class ShardedDbOpsResource
    extends AbstractCustomResourceService<ShardedDbOpsDto, StackGresShardedDbOps> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ShardedDbOpsDto.class))})
  @Override
  public List<ShardedDbOpsDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedDbOpsDto.class))})
  @Override
  public ShardedDbOpsDto create(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Override
  public void delete(ShardedDbOpsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedDbOpsDto.class))})
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
