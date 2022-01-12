/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("version")
@RequestScoped
public class VersionsResource {

  @Operation(responses = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(type = "object"))})
  })
  @CommonApiResponses
  @GET
  @Path("postgresql")
  public Map<String, List<String>> supportedPostgresVersions(@QueryParam("flavor") String flavor) {
    return Map.of(
        "postgresql", getPostgresFlavorComponent(flavor).getLatest().getOrderedVersions().toList());
  }

}
