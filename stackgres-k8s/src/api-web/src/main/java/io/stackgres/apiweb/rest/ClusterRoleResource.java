/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.clusterrole.ClusterRoleDto;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

@Path("clusterroles")
@RequestScoped
@Authenticated
public class ClusterRoleResource
    extends AbstractResourceService<ClusterRoleDto, ClusterRole> {

  private final ResourceScanner<ClusterRole> clusterRoleScanner;

  @Inject
  public ClusterRoleResource(ResourceScanner<ClusterRole> clusterRoleScanner) {
    this.clusterRoleScanner = clusterRoleScanner;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ClusterRoleDto.class))) })
      })
  @Override
  public List<ClusterRoleDto> list() {
    return clusterRoleScanner
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
                  schema = @Schema(implementation = ClusterRoleDto.class)) })
      })
  @Override
  public ClusterRoleDto create(@Valid ClusterRoleDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(@Valid ClusterRoleDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ClusterRoleDto.class)) })
      })
  @Override
  public ClusterRoleDto update(@Valid ClusterRoleDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected Optional<ClusterRole> findResource(ClusterRoleDto resource) {
    return finder.findByName(
        resource.getMetadata().getName());
  }

  @Override
  protected void updateSpec(
      ClusterRole resourceToUpdate, ClusterRole resource) {
    resourceToUpdate.setRules(resource.getRules());
  }

}
