/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedbackup.ShardedBackupDto;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgshardedbackups")
@RequestScoped
@Authenticated
public class NamespacedShardedBackupResource
    extends AbstractNamespacedRestService<ShardedBackupDto, StackGresShardedBackup> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedBackupDto.class))})
  @Override
  public ShardedBackupDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
