/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("kubernetes-cluster-info")
@RequestScoped
@Authenticated
public class KubernetesInfoResource {

  @Inject
  KubernetesClient client;

  /**
   * Return kubernetes cluster info.
   */
  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.STRING))})
  @CommonApiResponses
  @GET
  public Response info() {
    return Response.ok().entity(client.getMasterUrl()).build();
  }
}
