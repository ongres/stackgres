/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("sgshardedbackups")
@RequestScoped
@Authenticated
public class ShardedBackupResource
    extends AbstractCustomResourceService<ShardedBackupDto, StackGresShardedBackup> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ShardedBackupDto.class))})
  @Override
  public List<ShardedBackupDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedBackupDto.class))})
  @Override
  public ShardedBackupDto create(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Override
  public void delete(ShardedBackupDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedBackupDto.class))})
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
