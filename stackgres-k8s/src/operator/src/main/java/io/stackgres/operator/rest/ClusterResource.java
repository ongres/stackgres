/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.distributedlogs.DistributedLogsFetcher;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterLogEntryDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;
import org.jooq.lambda.Seq;

@Path("/stackgres/sgcluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource
    extends AbstractRestService<ClusterDto, StackGresCluster> {

  private final CustomResourceScanner<ClusterDto> clusterScanner;
  private final CustomResourceFinder<ClusterDto> clusterFinder;
  private final CustomResourceFinder<ClusterResourceConsumtionDto> clusterResourceConsumptionFinder;
  private final DistributedLogsFetcher distributedLogsFetcher;

  @Inject
  public ClusterResource(
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      ResourceTransformer<ClusterDto, StackGresCluster> transformer,
      CustomResourceScanner<ClusterDto> clusterScanner,
      CustomResourceFinder<ClusterDto> clusterFinder,
      CustomResourceFinder<ClusterResourceConsumtionDto> clusterResourceConsumptionFinder,
      DistributedLogsFetcher distributedLogsFetcher) {
    super(null, finder, scheduler, transformer);
    this.clusterScanner = clusterScanner;
    this.clusterFinder = clusterFinder;
    this.clusterResourceConsumptionFinder = clusterResourceConsumptionFinder;
    this.distributedLogsFetcher = distributedLogsFetcher;
  }

  public ClusterResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.clusterScanner = null;
    this.clusterFinder = null;
    this.clusterResourceConsumptionFinder = null;
    this.distributedLogsFetcher = null;
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

  /**
   * Query distributed logs and return a list of {@code ClusterLogEntry}.
   */
  @GET
  @Path("/logs/{namespace}/{name}")
  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  public List<ClusterLogEntryDto> logs(
      @PathParam("namespace") String namespace,
      @PathParam("name") String name,
      @QueryParam("records") Integer records,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @QueryParam("filters") String filters,
      @QueryParam("sort") String sort,
      @QueryParam("text") String text) {
    final ClusterDto cluster = super.get(namespace, name);
    final ImmutableMap<String, String> filterList = Optional.ofNullable(filters)
        .map(f -> Seq.of(f.split(","))
            .map(fieldValue -> Seq.of(fieldValue.split(":")).toList())
            .peek(list -> Preconditions.checkArgument(
                list.size() == 2, "You have to specify"
                    + " a field-value pair separating field name and value with character ':'"
                    + " and separate field-value pair with character ','"))
            .collect(ImmutableMap.toImmutableMap(list -> list.get(0), list -> list.get(1))))
        .orElse(ImmutableMap.of());
    return distributedLogsFetcher.logs(
        cluster,
        Optional.ofNullable(records).orElse(50),
        Optional.ofNullable(from).map(Instant::parse).orElse(null),
        Optional.ofNullable(to).map(Instant::parse).orElse(null),
        filterList,
        Objects.equals("asc", sort),
        text);
  }

}
