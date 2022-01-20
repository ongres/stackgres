/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ApplicationDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("application")
@RequestScoped
@Authenticated
public class ApplicationsResource {

  @Operation(responses = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(
              mediaType = MediaType.APPLICATION_JSON,
              schema = @Schema(type = "object"))})
  })
  @CommonApiResponses
  @GET
  public List<ApplicationDto> getAllApplications() {
    // Placeholder for applications
    return List.of();
  }

}
