/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgshardedbackups")
@RequestScoped
@Authenticated
public class NamespacedShardedBackupResource
    extends AbstractNamespacedRestService<ShardedBackupDto, StackGresShardedBackup> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ShardedBackupDto.class))})
      })
  @Override
  public ShardedBackupDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
