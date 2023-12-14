/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sginstanceprofile;

import java.util.List;
import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractCustomResourceServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sginstanceprofiles")
@RequestScoped
@Authenticated
@Tag(name = "sginstanceprofile")
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
public class ProfileResource
    extends AbstractCustomResourceServiceDependency<ProfileDto, StackGresProfile> {

  @Override
  public boolean belongsToCluster(StackGresProfile resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getSgInstanceProfile(),
            resource.getMetadata().getName());
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ProfileDto.class))})
  @Operation(summary = "List sginstanceprofiles", description = """
      List sginstanceprofiles.

      ### RBAC permissions required

      * sginstanceprofiles list
      """)
  @Override
  public List<ProfileDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProfileDto.class))})
  @Operation(summary = "Create a sginstanceprofile", description = """
      Create a sginstanceprofile.

      ### RBAC permissions required

      * sginstanceprofiles create
      """)
  @Override
  public ProfileDto create(ProfileDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Operation(summary = "Delete a sginstanceprofile", description = """
      Delete a sginstanceprofile.

      ### RBAC permissions required

      * sginstanceprofiles delete
      """)
  @Override
  public void delete(ProfileDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProfileDto.class))})
  @Operation(summary = "Update a sginstanceprofile", description = """
      Update a sginstanceprofile.

      ### RBAC permissions required

      * sginstanceprofiles patch
      """)
  @Override
  public ProfileDto update(ProfileDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresProfile resourceToUpdate, StackGresProfile resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
