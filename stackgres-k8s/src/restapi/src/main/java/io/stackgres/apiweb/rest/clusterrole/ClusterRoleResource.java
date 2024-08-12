/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.clusterrole;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.clusterrole.ClusterRoleDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractResourceService;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("clusterroles")
@RequestScoped
@Authenticated
@Tag(name = "clusterrole")
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
public class ClusterRoleResource
    extends AbstractResourceService<ClusterRoleDto, ClusterRole> {

  private final ResourceScanner<ClusterRole> clusterRoleScanner;

  @Inject
  public ClusterRoleResource(ResourceScanner<ClusterRole> clusterRoleScanner) {
    this.clusterRoleScanner = clusterRoleScanner;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ClusterRoleDto.class))})
  @Operation(summary = "List clusterroles", description = """
      List clusterroles.

      ### RBAC permissions required

      * clusterroles list
      """)
  @Override
  public List<ClusterRoleDto> list() {
    return clusterRoleScanner
        .getResourcesWithLabels(
            Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE))
        .stream()
        .map(transformer::toDto)
        .toList();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ClusterRoleDto.class))})
  @Operation(summary = "Create a cluster role", description = """
      Create a cluster role.

      ### RBAC permissions required

      * clusterrole create
      """)
  @Override
  public ClusterRoleDto create(@Valid ClusterRoleDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a cluster role", description = """
      Delete a cluster role.

      ### RBAC permissions required

      * clusterrole delete
      """)
  @Override
  public void delete(@Valid ClusterRoleDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ClusterRoleDto.class))})
  @Operation(summary = "Update a cluster role", description = """
      Update a cluster role.

      ### RBAC permissions required

      * clusterrole patch
      """)
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
