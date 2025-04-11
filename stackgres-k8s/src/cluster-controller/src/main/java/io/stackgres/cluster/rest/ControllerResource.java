/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.rest;

import java.net.URI;

import io.stackgres.cluster.controller.PatroniReconciliator;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.WebClientFactory;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("controller")
@RequestScoped
@Tag(name = "probe")
@APIResponse(responseCode = "500", description = "Internal Server Error")
public class ControllerResource {

  @Inject
  PatroniReconciliator patroniReconciliator;

  @Inject
  WebClientFactory webClientFactory;

  @APIResponse(responseCode = "200", description = "When live")
  @Operation(summary = "Check if live", description = "Check if live")
  @GET
  @Path("liveness")
  public Response liveness() {
    if (!patroniReconciliator.isStartup()) {
      return Response.ok().build();
    }
    final URI uri = URI.create("http://localhost:" + EnvoyUtil.PATRONI_PORT + "/liveness");
    try {
      return webClientFactory.create(uri).get(uri);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
