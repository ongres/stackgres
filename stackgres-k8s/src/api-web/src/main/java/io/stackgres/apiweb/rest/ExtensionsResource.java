/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.extension.ExtensionsDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.apiweb.transformer.ExtensionsTransformer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("extensions")
@RequestScoped
@Authenticated
public class ExtensionsResource {

  private final ExtensionMetadataManager extensionMetadataManager;
  private final ExtensionsTransformer extensionsTransformer;

  @Inject
  public ExtensionsResource(
      ExtensionMetadataManager extensionMetadataManager,
      ExtensionsTransformer extensionsTransformer) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.extensionsTransformer = extensionsTransformer;
  }

  /**
   * Looks for all extensions that are published in configured repositories with only versions
   * available for the sgcluster retrieved using the namespace and name provided.
   * @return the extensions
   */
  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ExtensionsDto.class)) })
      })
  @CommonApiResponses
  @GET
  @Path("{postgresVersion}")
  public ExtensionsDto get(@PathParam("postgresVersion") String postgresVersion,
      @QueryParam("flavor") String flavor) {
    StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().getPostgres().setVersion(postgresVersion);
    cluster.getSpec().getPostgres().setFlavor(flavor);
    var extensionMetadataList = extensionMetadataManager.getExtensions();
    return extensionsTransformer.toDto(extensionMetadataList, cluster);
  }

}
