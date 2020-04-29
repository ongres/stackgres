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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.distributedlogs.DistributedLogsFetcher;
import io.stackgres.operator.rest.distributedlogs.FullTextSearchQuery;
import io.stackgres.operator.rest.distributedlogs.ImmutableDistributedLogsQueryParameters;
import io.stackgres.operator.rest.dto.cluster.ClusterDistributedLogs;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterLogEntryDto;
import io.stackgres.operator.rest.dto.cluster.ClusterResourceConsumtionDto;
import io.stackgres.operator.rest.dto.cluster.ClusterSpec;
import io.stackgres.operator.rest.transformer.ResourceTransformer;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Path("/stackgres/sgcluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterResource
    extends AbstractRestService<ClusterDto, StackGresCluster> {

  private final CustomResourceScanner<ClusterDto> clusterScanner;
  private final CustomResourceFinder<ClusterDto> clusterFinder;
  private final CustomResourceFinder<ClusterResourceConsumtionDto> clusterResourceConsumptionFinder;
  private final DistributedLogsFetcher distributedLogsFetcher;
  private final ObjectMapper objectMapper;

  @Inject
  public ClusterResource(
      CustomResourceFinder<StackGresCluster> finder,
      CustomResourceScheduler<StackGresCluster> scheduler,
      ResourceTransformer<ClusterDto, StackGresCluster> transformer,
      CustomResourceScanner<ClusterDto> clusterScanner,
      CustomResourceFinder<ClusterDto> clusterFinder,
      CustomResourceFinder<ClusterResourceConsumtionDto> clusterResourceConsumptionFinder,
      DistributedLogsFetcher distributedLogsFetcher,
      ObjectMapperProvider objectMapperProvider) {
    super(null, finder, scheduler, transformer);
    this.clusterScanner = clusterScanner;
    this.clusterFinder = clusterFinder;
    this.clusterResourceConsumptionFinder = clusterResourceConsumptionFinder;
    this.distributedLogsFetcher = distributedLogsFetcher;
    this.objectMapper = objectMapperProvider.objectMapper();
  }

  public ClusterResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.clusterScanner = null;
    this.clusterFinder = null;
    this.clusterResourceConsumptionFinder = null;
    this.distributedLogsFetcher = null;
    this.objectMapper = null;
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
    final ImmutableMap<String, Optional<String>> filterList;
    final Optional<Tuple2<Instant, Integer>> fromTuple;
    final Optional<Tuple2<Instant, Integer>> toTuple;

    if (!Optional.ofNullable(cluster.getSpec())
        .map(ClusterSpec::getDistributedLogs)
        .map(ClusterDistributedLogs::getDistributedLogs)
        .isPresent()) {
      throw new BadRequestException(
          "Distributed logs are not configured for specified cluster");
    }

    try {
      filterList = Optional.ofNullable(filters)
          .map(Unchecked.function(objectMapper::readTree))
          .map(node -> Seq.seq(node.fields())
              .collect(ImmutableMap.toImmutableMap(
                  e -> e.getKey(), e -> (Optional<String>) Optional.of(e.getValue())
                  .filter(JsonNode::isNull)
                  .map(JsonNode::asText))))
          .orElse(ImmutableMap.<String, Optional<String>>of());
    } catch (Exception ex) {
      throw new BadRequestException("filters should be a JSON object", ex);
    }

    try {
      fromTuple = Optional.ofNullable(from)
          .map(s -> s.split(","))
          .map(ss -> Tuple.tuple(ss[0], ss.length > 1 ? ss[1] : "1"))
          .map(t -> t.map1(Instant::parse))
          .map(t -> t.map2(Integer::valueOf));
    } catch (Exception ex) {
      throw new BadRequestException("from should be a timestamp"
          + " or a timestamp and an index separated by character ','", ex);
    }

    try {
      toTuple = Optional.ofNullable(to)
          .map(s -> s.split(","))
          .map(ss -> Tuple.tuple(ss[0], ss.length > 1 ? ss[1] : String.valueOf(Integer.MAX_VALUE)))
          .map(t -> t.map1(Instant::parse))
          .map(t -> t.map2(Integer::valueOf));
    } catch (Exception ex) {
      throw new BadRequestException("to should be a timestamp"
          + " or a timestamp and an index separated by character ','", ex);
    }

    if (sort != null && !sort.equals("asc") && !sort.equals("desc")) {
      throw new BadRequestException("sort only accept asc or desc values");
    }

    return distributedLogsFetcher.logs(
        ImmutableDistributedLogsQueryParameters.builder()
        .cluster(cluster)
        .records(Optional.ofNullable(records).orElse(50))
        .fromTimeAndIndex(fromTuple)
        .toTimeAndIndex(toTuple)
        .filters(filterList)
        .isSortAsc(Objects.equals("asc", sort))
        .fullTextSearchQuery(Optional.ofNullable(text)
            .map(FullTextSearchQuery::new))
        .build());
  }
}
