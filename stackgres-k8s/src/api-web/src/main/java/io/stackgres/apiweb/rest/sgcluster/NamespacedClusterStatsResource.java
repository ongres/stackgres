/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgcluster;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterStatsDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.resource.ClusterStatsDtoFinder;
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

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgclusters")
@RequestScoped
@Authenticated
@Tag(name = "sgcluster")
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
public class NamespacedClusterStatsResource {

  private final ClusterStatsDtoFinder clusterResourceStatsFinder;

  @Inject
  public NamespacedClusterStatsResource(
      ClusterStatsDtoFinder clusterResourceStatsFinder) {
    this.clusterResourceStatsFinder = clusterResourceStatsFinder;
  }

  /**
   * Return a {@code ClusterStatus}.
   */
  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ClusterStatsDto.class))})
  @Operation(summary = "Get a sgcluster's stats", description = """
      Get a sgcluster's stats.

      ### RBAC permissions required

      * sgclusters get
      * pod list
      * services list
      * pod/exec create
      * persistentvolume list
      """)
  @GET
  @Path("{name}/stats")
  public ClusterStatsDto stats(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    return clusterResourceStatsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

}
