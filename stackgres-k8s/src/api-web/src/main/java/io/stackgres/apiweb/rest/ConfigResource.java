/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jetbrains.annotations.NotNull;

@Path("sgconfigs")
@RequestScoped
@Authenticated
public class ConfigResource extends AbstractRestService<ConfigDto,
    StackGresConfig> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(
                      implementation = ConfigDto.class
                  )))})
      })
  @Override
  public @NotNull List<ConfigDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(@NotNull ConfigDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(@NotNull ConfigDto resource) {
    super.update(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(@NotNull ConfigDto resource) {
    super.delete(resource);
  }

  @Override
  protected void updateSpec(StackGresConfig resourceToUpdate,
      StackGresConfig resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
