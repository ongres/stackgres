/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.transformer.ExtensionsTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.docir.DocirMetadataManager;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("extensions")
@RequestScoped
@Authenticated
@Tag(name = "misc")
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
public class ExtensionsResource {

  private final DocirMetadataManager docirMetadataManager;
  private final ExtensionsTransformer extensionsTransformer;

  @Inject
  public ExtensionsResource(
      DocirMetadataManager docirMetadataManager,
      ExtensionsTransformer extensionsTransformer) {
    this.docirMetadataManager = docirMetadataManager;
    this.extensionsTransformer = extensionsTransformer;
  }

  /**
   * Looks for all extensions that are published in the docir repository with only versions
   * available for a new SGCluster (that retrieves its images from the StackGres images registry)
   * with the given Postgres version and flavor.
   *
   * @return the extensions
   */
  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ExtensionsDto.class))})
  @Operation(summary = "List PostgreSQL extensions", description = """
      List PostgreSQL extensions.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("{postgresVersion}")
  public ExtensionsDto get(
      @PathParam("postgresVersion") String postgresVersion,
      @QueryParam("flavor") String flavor) {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().getPostgres().setVersion(postgresVersion);
    cluster.getSpec().getPostgres().setFlavor(flavor);
    cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    cluster.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    cluster.getSpec().getConfigurations().getRegistry().setEnabled(true);
    var extensionMetadataList = docirMetadataManager.getExtensions();
    return extensionsTransformer.toDto(extensionMetadataList, cluster);
  }

}
