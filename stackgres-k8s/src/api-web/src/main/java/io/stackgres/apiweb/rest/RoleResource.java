/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.Role;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.role.RoleDto;
import io.stackgres.common.StackGresContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

@Path("roles")
@RequestScoped
@Authenticated
public class RoleResource
    extends AbstractResourceService<RoleDto, Role> {

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = RoleDto.class))) })
      })
  @Override
  public List<RoleDto> list() {
    return scanner
        .findByLabels(
            Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .stream()
        .map(transformer::toDto)
        .toList();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = RoleDto.class)) })
      })
  @Override
  public RoleDto create(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = RoleDto.class)) })
      })
  @Override
  public RoleDto update(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(
      Role resourceToUpdate, Role resource) {
    resourceToUpdate.setRules(resource.getRules());
  }

}
