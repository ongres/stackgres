/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

@Path("/stackgres/sgcluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource
    extends AbstractRestService<ClusterDto, StackGresCluster> {

  private final CustomResourceScanner<ClusterDto> clusterScanner;
  private final CustomResourceFinder<ClusterDto> clusterFinder;
  private final CustomResourceFinder<ClusterResourceConsumtionDto> clusterResourceConsumptionFinder;

  @Inject
  public ClusterResource(
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      ResourceTransformer<ClusterDto, StackGresCluster> transformer,
      CustomResourceScanner<ClusterDto> clusterScanner,
      CustomResourceFinder<ClusterDto> clusterFinder,
      CustomResourceFinder<ClusterResourceConsumtionDto> clusterResourceConsumptionFinder) {
    super(null, finder, scheduler, transformer);
    this.clusterScanner = clusterScanner;
    this.clusterFinder = clusterFinder;
    this.clusterResourceConsumptionFinder = clusterResourceConsumptionFinder;
  }

  public ClusterResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.clusterScanner = null;
    this.clusterFinder = null;
    this.clusterResourceConsumptionFinder = null;
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
  public List<ClusterDto> list() {
    return clusterScanner.getResources();
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
  public ClusterDto get(String namespace, String name) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Return a {@code ClusterStatus}.
   */
  @GET
  @Path("/status/{namespace}/{name}")
  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  public ClusterResourceConsumtionDto status(@PathParam("namespace") String namespace,
                                             @PathParam("name") String name) {
    return clusterResourceConsumptionFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

}
