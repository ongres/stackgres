/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.secret.SecretDto;
import io.stackgres.common.resource.ResourceScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("")
@RequestScoped
public class SecretResource {

  private final ResourceScanner<SecretDto> scanner;

  @Inject
  public SecretResource(ResourceScanner<SecretDto> scanner) {
    this.scanner = scanner;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = SecretDto.class))) })
      })
  @CommonApiResponses
  @Authenticated
  @GET
  @Path("{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/secrets")
  public List<SecretDto> list(@PathParam("namespace") String namespace) {
    return scanner.findResourcesInNamespace(namespace);
  }
}
