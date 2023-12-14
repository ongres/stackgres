/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.role;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.Role;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.role.RoleDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractResourceService;
import io.stackgres.common.StackGresContext;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("roles")
@RequestScoped
@Authenticated
@Tag(name = "role")
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
public class RoleResource
    extends AbstractResourceService<RoleDto, Role> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = RoleDto.class))})
  @Operation(summary = "List roles", description = """
      List roles.

      ### RBAC permissions required

      * roles list
      """)
  @Override
  public List<RoleDto> list() {
    return scanner
        .findByLabels(
            Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .stream()
        .map(transformer::toDto)
        .toList();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = RoleDto.class))})
  @Operation(summary = "Create a role", description = """
      Create a role.

      ### RBAC permissions required

      * role create
      """)
  @Override
  public RoleDto create(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a role", description = """
      Delete a role.

      ### RBAC permissions required

      * role delete
      """)
  @Override
  public void delete(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = RoleDto.class))})
  @Operation(summary = "Update a role", description = """
      Update a role.

      ### RBAC permissions required

      * role patch
      """)
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
