/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/stackgres/sgpoolconfig")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionPoolingConfigResource extends
    AbstractDependencyRestService<PoolingConfigDto, StackGresPoolingConfig> {

  @Inject
  public ConnectionPoolingConfigResource(
      CustomResourceScanner<StackGresPoolingConfig> scanner,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      DependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> transformer) {
    super(scanner, finder, scheduler, clusterScanner, transformer);
  }

  public ConnectionPoolingConfigResource() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

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
  @CommonApiResponses
  @Authenticated
  @Override
  public List<PoolingConfigDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = PoolingConfigDto.class)) })
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public PoolingConfigDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void create(PoolingConfigDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void delete(PoolingConfigDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void update(PoolingConfigDto resource) {
    super.update(resource);
  }

}
