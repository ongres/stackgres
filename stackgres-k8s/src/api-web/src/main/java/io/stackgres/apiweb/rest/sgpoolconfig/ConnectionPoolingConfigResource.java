/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgpoolconfig;

import java.util.List;
import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.rest.AbstractCustomResourceServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgpoolconfigs")
@RequestScoped
@Authenticated
public class ConnectionPoolingConfigResource extends
    AbstractCustomResourceServiceDependency<PoolingConfigDto, StackGresPoolingConfig> {

  @Override
  public boolean belongsToCluster(StackGresPoolingConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfigurations().getSgPoolingConfig(),
            resource.getMetadata().getName());
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = PoolingConfigDto.class))})
  @Tag(name = "sgpoolconfig")
  @Operation(summary = "List sgpoolconfigs", description = """
      List sgpoolconfigs.

      ### RBAC permissions required

      * sgpoolconfigs list
      """)
  @Override
  public List<PoolingConfigDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PoolingConfigDto.class))})
  @Tag(name = "sgpoolconfig")
  @Operation(summary = "Create a sgpoolconfig", description = """
      Create a sgpoolconfig.

      ### RBAC permissions required

      * sgpoolconfig create
      """)
  @Override
  public PoolingConfigDto create(PoolingConfigDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Tag(name = "sgpoolconfig")
  @Operation(summary = "Delete a sgpoolconfig", description = """
      Delete a sgpoolconfig.

      ### RBAC permissions required

      * sgpoolconfig delete
      """)
  @Override
  public void delete(PoolingConfigDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PoolingConfigDto.class))})
  @Tag(name = "sgpoolconfig")
  @Operation(summary = "Update a sgpoolconfig", description = """
      Update a sgpoolconfig.

      ### RBAC permissions required

      * sgpoolconfig patch
      """)
  @Override
  public PoolingConfigDto update(PoolingConfigDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresPoolingConfig resourceToUpdate,
      StackGresPoolingConfig resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
