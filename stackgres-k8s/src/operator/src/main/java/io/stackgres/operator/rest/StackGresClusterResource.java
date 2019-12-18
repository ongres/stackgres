/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterList;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.dto.ClusterPodConfig;
import io.stackgres.operator.resource.dto.ClusterResourceConsumtion;

@Path("/stackgres/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresClusterResource
    extends AbstractCustomResourceRestService<StackGresCluster, StackGresClusterList> {

  @Inject
  KubernetesCustomResourceFinder<ClusterResourceConsumtion> statusFinder;

  @Inject
  KubernetesCustomResourceFinder<ClusterPodConfig> detailsFinder;

  public StackGresClusterResource() {
    super(StackGresClusterDefinition.NAME);
  }

  /**
   * Return a {@code ClusterStatus}.
   */
  @Path("/status/{namespace}/{name}")
  @GET
  public ClusterResourceConsumtion status(@PathParam("namespace") String namespace,
                                          @PathParam("name") String name) {

    return statusFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);

  }

  @Path("/pods/{namespace}/{name}")
  @GET
  public ClusterPodConfig details(@PathParam("namespace") String namespace,
                                  @PathParam("name") String name) {
    return detailsFinder.findByNameAndNamespace(namespace, name)
        .orElseThrow(NotFoundException::new);
  }

}
