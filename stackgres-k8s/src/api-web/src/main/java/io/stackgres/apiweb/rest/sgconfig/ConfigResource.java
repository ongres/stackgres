/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgconfig;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.apiweb.rest.AbstractCustomResourceService;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;

@Path("sgconfigs")
@RequestScoped
@Authenticated
public class ConfigResource extends AbstractCustomResourceService<ConfigDto, StackGresConfig> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ConfigDto.class))})
  @Tag(name = "sgconfig")
  @Operation(summary = "List sgconfigs", description = """
      List sgconfigs and read values from the referenced secrets.

      ### RBAC permissions required

      * sgconfigs list
      """)
  @Override
  public @NotNull List<ConfigDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ConfigDto.class))})
  @Tag(name = "sgconfig")
  @Operation(summary = "Create a sgconfigs", description = """
        Create a sgconfigs.

        ### RBAC permissions required

        * sgconfigs create
      """)
  @Override
  public ConfigDto create(@NotNull ConfigDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ConfigDto.class))})
  @Tag(name = "sgconfig")
  @Operation(summary = "Update a sgconfigs", description = """
      Update a sgconfigs.

      ### RBAC permissions required

      * sgconfigs patch
      """)
  @Override
  public ConfigDto update(@NotNull ConfigDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Tag(name = "sgconfig")
  @Operation(summary = "Delete a sgconfigs", description = """
      Delete a sgconfigs.

      ### RBAC permissions required

      * sgconfigs delete
      """)
  @Override
  public void delete(@NotNull ConfigDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresConfig resourceToUpdate,
      StackGresConfig resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
