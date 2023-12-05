/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgshardeddbops;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardeddbops.ShardedDbOpsDto;
import io.stackgres.apiweb.rest.AbstractNamespacedRestService;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgshardeddbops")
@RequestScoped
@Authenticated
public class NamespacedShardedDbOpsResource
    extends AbstractNamespacedRestService<ShardedDbOpsDto, StackGresShardedDbOps> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedDbOpsDto.class))})
  @Tag(name = "sgshardeddbops")
  @Operation(summary = "Get a sgshardeddbops", description = """
      Get a sgshardeddbops.

      ### RBAC permissions required

      * sgshardeddbops get
      """)
  @Override
  public ShardedDbOpsDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
