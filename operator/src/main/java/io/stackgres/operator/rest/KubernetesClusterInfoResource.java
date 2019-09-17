/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

@Path("/kubernetes-cluster-info")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class KubernetesClusterInfoResource {

  @Inject
  KubernetesClientFactory kubeClient;

  /**
   * Return kubernetes cluster info.
   */
  @GET
  public Response info() {
    try (KubernetesClient client = kubeClient.create()) {
      return Response.ok().entity(client.settings().getMasterUrl()).build();
    }
  }
}
