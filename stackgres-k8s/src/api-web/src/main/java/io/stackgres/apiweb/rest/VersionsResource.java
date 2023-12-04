/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("version")
@RequestScoped
@Authenticated
public class VersionsResource {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = MediaType.APPLICATION_JSON,
          schema = @Schema(type = SchemaType.OBJECT))})
  @CommonApiResponses
  @GET
  @Path("postgresql")
  public Map<String, List<String>> supportedPostgresVersions(@QueryParam("flavor") String flavor) {
    return Map.of(
        "postgresql",
        getPostgresFlavorComponent(flavor).getLatest().streamOrderedVersions().toList());
  }

}
