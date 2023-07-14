/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@Path("sgpoolconfigs")
@RequestScoped
@Authenticated
public class ConnectionPoolingConfigResource extends
    AbstractRestServiceDependency<PoolingConfigDto, StackGresPoolingConfig> {

  @Override
  public boolean belongsToCluster(StackGresPoolingConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfiguration().getConnectionPoolingConfig(),
            resource.getMetadata().getName());
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = PoolingConfigDto.class))) })
      })
  @Override
  public List<PoolingConfigDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(PoolingConfigDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(PoolingConfigDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(PoolingConfigDto resource) {
    super.update(resource);
  }

  @Override
  protected void updateSpec(StackGresPoolingConfig resourceToUpdate,
      StackGresPoolingConfig resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
