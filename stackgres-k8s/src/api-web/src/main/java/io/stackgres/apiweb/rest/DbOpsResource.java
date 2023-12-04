/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("sgdbops")
@RequestScoped
@Authenticated
public class DbOpsResource
    extends AbstractCustomResourceService<DbOpsDto, StackGresDbOps> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = DbOpsDto.class))})

  @Override
  public List<DbOpsDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = DbOpsDto.class))})
  @Override
  public DbOpsDto create(DbOpsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Override
  public void delete(DbOpsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = DbOpsDto.class))})
  @Override
  public DbOpsDto update(DbOpsDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresDbOps resourceToUpdate, StackGresDbOps resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
