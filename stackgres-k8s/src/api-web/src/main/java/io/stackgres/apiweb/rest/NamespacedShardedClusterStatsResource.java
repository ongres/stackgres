/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.CustomResourceFinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgshardedclusters")
@RequestScoped
@Authenticated
public class NamespacedShardedClusterStatsResource {

  private final CustomResourceFinder<ShardedClusterStatsDto> shardedClusterResourceStatsFinder;

  @Inject
  public NamespacedShardedClusterStatsResource(
      CustomResourceFinder<ShardedClusterStatsDto> shardedClusterResourceStatsFinder) {
    this.shardedClusterResourceStatsFinder = shardedClusterResourceStatsFinder;
  }

  /**
   * Return a {@code ClusterStatus}.
   */
  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ShardedClusterStatsDto.class))})
      })
  @CommonApiResponses
  @GET
  @Path("{name}/stats")
  public ShardedClusterStatsDto stats(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    return shardedClusterResourceStatsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

}
