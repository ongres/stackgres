/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
          schema = @Schema(type = SchemaType.STRING,
              description = "The URL to connect to the kubernetes cluster"))})
  @Tag(name = "misc")
  @Operation(summary = "Get kubernetes cluster info", description = """
      Get kubernetes cluster info.

      ### RBAC permissions required

      None
      """)
  @CommonApiResponses
  @GET
  public Response info() {
    return Response.ok().entity(client.getMasterUrl()).build();
  }
}
