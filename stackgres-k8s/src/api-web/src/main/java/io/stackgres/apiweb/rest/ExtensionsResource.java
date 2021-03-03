/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.transformer.ExtensionsTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jooq.lambda.Unchecked;

@Path("/stackgres/extensions")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExtensionsResource {

  private final ClusterExtensionMetadataManager clusterExtensionMetadataManager;
  private final ExtensionsTransformer extensionsTransformer;

  @Inject
  public ExtensionsResource(
      ClusterExtensionMetadataManager clusterExtensionMetadataManager,
      ExtensionsTransformer extensionsTransformer) {
    this.clusterExtensionMetadataManager = clusterExtensionMetadataManager;
    this.extensionsTransformer = extensionsTransformer;
  }

  /**
   * Looks for all extensions that are published in configured repositories with only versions
   * available for the sgcluster retrieved using the namespace and name provided.
   * @return the extensions
   */
  @Path("/{postgresVersion}")
  @GET
  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ExtensionsDto.class)) })
      })
  @CommonApiResponses
  @Authenticated
  public ExtensionsDto get(@PathParam("postgresVersion") String postgresVersion) {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgresVersion(postgresVersion);
    return Optional.of(Unchecked.supplier(() -> clusterExtensionMetadataManager
            .getExtensions()).get())
        .map(extensionMetadataList -> extensionsTransformer.toDto(extensionMetadataList, cluster))
        .get();
  }

}
