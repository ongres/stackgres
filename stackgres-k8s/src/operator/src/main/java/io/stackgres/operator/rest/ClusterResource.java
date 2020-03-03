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

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterPodConfigDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

@Path("/stackgres/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource
    extends AbstractRestService<ClusterDto, StackGresCluster> {

  final CustomResourceFinder<ClusterResourceConsumtionDto> statusFinder;
  final CustomResourceFinder<ClusterPodConfigDto> detailsFinder;

  @Inject
  public ClusterResource(
      CustomResourceScanner<StackGresCluster> scanner,
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      ResourceTransformer<ClusterDto, StackGresCluster> transformer,
      CustomResourceFinder<ClusterResourceConsumtionDto> statusFinder,
      CustomResourceFinder<ClusterPodConfigDto> detailsFinder) {
    super(scanner, finder, scheduler, transformer);
    this.statusFinder = statusFinder;
    this.detailsFinder = detailsFinder;
  }

  public ClusterResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.statusFinder = null;
    this.detailsFinder = null;
  }

  /**
   * Return a {@code ClusterStatus}.
   */
  @Path("/status/{namespace}/{name}")
  @GET
  public ClusterResourceConsumtionDto status(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    return statusFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

  @Path("/pods/{namespace}/{name}")
  @GET
  public ClusterPodConfigDto details(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    return detailsFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

}
