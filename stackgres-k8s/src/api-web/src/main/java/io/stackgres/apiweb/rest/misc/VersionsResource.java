/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.exception.ErrorResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("version")
@RequestScoped
@Authenticated
@Tag(name = "misc")
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
public class VersionsResource {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = MediaType.APPLICATION_JSON,
          schema = @Schema(type = SchemaType.OBJECT))})
  @Operation(summary = "List postgres versions", description = """
      List of the supported postgres versions.

      Allowed values that can be used in the SGCluster postgresVersion definition.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("postgresql")
  public Map<String, List<String>> supportedPostgresVersions(@QueryParam("flavor") String flavor) {
    return Map.of(
        "postgresql",
        getPostgresFlavorComponent(flavor).getLatest().streamOrderedVersions().toList());
  }

}
