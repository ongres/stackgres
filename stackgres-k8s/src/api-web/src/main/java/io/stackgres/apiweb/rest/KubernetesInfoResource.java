/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.Authenticated;
import io.stackgres.common.KubernetesClientFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/stackgres/kubernetes-cluster-info")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KubernetesInfoResource {

  @Inject
  KubernetesClientFactory clientFactory;

  /**
   * Return kubernetes cluster info.
   */
  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(type = "string")) })
      })
  @CommonApiResponses
  @GET
  @Authenticated
  public Response info() {
    try (KubernetesClient client = clientFactory.create()) {
      return Response.ok().entity(client.getMasterUrl()).build();
    }
  }
}
