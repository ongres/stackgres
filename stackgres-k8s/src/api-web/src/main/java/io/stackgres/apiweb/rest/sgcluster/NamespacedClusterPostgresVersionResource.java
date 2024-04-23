/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgcluster;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
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
public class NamespacedClusterPostgresVersionResource {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public NamespacedClusterPostgresVersionResource(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = String.class))})
  @Operation(summary = "Get versions available for an already existing sgcluster", description = """
      Get versions available for an already existing sgcluster taking into account the version
       set.

      ### RBAC permissions required

      * sgcluster get
      """)
  @GET
  @Path("{name}/version/postgresql")
  public List<String> list(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    var cluster = clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
    return StackGresUtil.getPostgresFlavorComponent(cluster)
        .get(cluster)
        .streamOrderedVersions()
        .toList();
  }

}
