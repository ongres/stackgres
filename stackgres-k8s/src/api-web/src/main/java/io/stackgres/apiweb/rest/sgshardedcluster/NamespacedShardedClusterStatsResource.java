/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgshardedcluster;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterStatsDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ShardedClusterStatsDto.class))})
  @Tag(name = "sgshardedcluster")
  @Operation(summary = "Get a sgshardedcluster's stats", description = """
      Get a sgshardedcluster's stats.

      ### RBAC permissions required

      * sgshardedclusters get
      * pod list
      * persistentvolume list
      """)
  @CommonApiResponses
  @GET
  @Path("{name}/stats")
  public ShardedClusterStatsDto stats(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    return shardedClusterResourceStatsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

}
