/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgpgconfig;

import java.util.List;
import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.rest.AbstractCustomResourceServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgpgconfigs")
@RequestScoped
@Authenticated
public class PostgresConfigResource extends
    AbstractCustomResourceServiceDependency<PostgresConfigDto, StackGresPostgresConfig> {

  @Override
  public boolean belongsToCluster(StackGresPostgresConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfigurations().getSgPostgresConfig(),
            resource.getMetadata().getName());
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = PostgresConfigDto.class))})
  @Tag(name = "sgpgconfig")
  @Operation(summary = "List sgpgconfigs", description = """
      List sgpgconfigs.

      ### RBAC permissions required

      * sgpgconfigs list
      """)
  @Override
  public List<PostgresConfigDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostgresConfigDto.class))})
  @Tag(name = "sgpgconfig")
  @Operation(summary = "Create a sgpgconfig", description = """
      Create a sgpgconfig.

      ### RBAC permissions required

      * sgpgconfigs create
      """)
  @Override
  public PostgresConfigDto create(PostgresConfigDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Tag(name = "sgpgconfig")
  @Operation(summary = "Delete a sgpgconfig", description = """
      Delete a sgpgconfig.

      ### RBAC permissions required

      * sgpgconfigs delete
      """)
  @Override
  public void delete(PostgresConfigDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostgresConfigDto.class))})
  @Tag(name = "sgpgconfig")
  @Operation(summary = "Update a sgpgconfig", description = """
      Update a sgpgconfig.

      ### RBAC permissions required

      * sgpgconfigs patch
      """)
  @Override
  public PostgresConfigDto update(PostgresConfigDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresPostgresConfig resourceToUpdate,
      StackGresPostgresConfig resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
