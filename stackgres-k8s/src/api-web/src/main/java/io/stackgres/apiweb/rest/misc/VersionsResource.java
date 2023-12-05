/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
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
public class VersionsResource {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = MediaType.APPLICATION_JSON,
          schema = @Schema(type = SchemaType.OBJECT))})
  @Tag(name = "misc")
  @Operation(summary = "List postgres versions", description = """
      List of the supported postgres versions.

      Allowed values that can be used in the SGCluster postgresVersion definition.

      ### RBAC permissions required

      None
      """)
  @CommonApiResponses
  @GET
  @Path("postgresql")
  public Map<String, List<String>> supportedPostgresVersions(@QueryParam("flavor") String flavor) {
    return Map.of(
        "postgresql",
        getPostgresFlavorComponent(flavor).getLatest().streamOrderedVersions().toList());
  }

}
