/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.config.ConfigDto;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jetbrains.annotations.NotNull;

@Path("sgconfigs")
@RequestScoped
@Authenticated
public class ConfigResource extends AbstractCustomResourceService<ConfigDto, StackGresConfig> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ConfigDto.class))})
  @Override
  public @NotNull List<ConfigDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ConfigDto.class))})
  @Override
  public ConfigDto create(@NotNull ConfigDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ConfigDto.class))})
  @Override
  public ConfigDto update(@NotNull ConfigDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
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
