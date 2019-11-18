/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesResourceScanner;
import io.stackgres.operator.resource.dto.Cluster;

@Path("/stackgres/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresClusterResource {

  @Inject
  KubernetesClientFactory kubeClient;

  @Inject
  KubernetesResourceScanner<List<Cluster>> clusterScanner;

  @Inject
  CustomResourceScheduler<StackGresCluster> clusterScheduler;

  /**
   * Return the list of {@code StackGresCluster}.
   */
  @GET
  public List<Cluster> list() {

    return clusterScanner.findResources().orElse(new ArrayList<>());

  }

  /**
   * Return a {@code StackGresCluster}.
   */
  @Path("/{namespace}/{name}")
  @GET
  public Cluster get(@PathParam("namespace") String namespace,
                     @PathParam("name") String name) {

    return clusterScanner
        .findResources(namespace)
        .flatMap(clusters -> clusters.stream()
            .filter(c -> c.getMetadata().getName().equals(name))
            .findFirst()
        ).orElseThrow(NotFoundException::new);

  }

  /**
   * Create a {@code StackGresCluster}.
   */
  @POST
  public void create(StackGresCluster cluster) {
    clusterScheduler.create(cluster);
  }

  /**
   * Delete a {@code StackGresCluster}.
   */
  @DELETE
  public void delete(StackGresCluster cluster) {
    clusterScheduler.delete(cluster);
  }

  /**
   * Create or update a {@code StackGresCluster}.
   */
  @PUT
  public void update(StackGresCluster cluster) {
    clusterScheduler.update(cluster);
  }

}
