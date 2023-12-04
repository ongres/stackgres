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
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("roles")
@RequestScoped
@Authenticated
public class RoleResource
    extends AbstractResourceService<RoleDto, Role> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = RoleDto.class))})
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
  @Override
  public RoleDto create(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Override
  public void delete(@Valid RoleDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = RoleDto.class))})
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
