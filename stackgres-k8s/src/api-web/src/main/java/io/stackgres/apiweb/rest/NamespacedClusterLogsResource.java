/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.distributedlogs.DistributedLogsFetcher;
import io.stackgres.apiweb.distributedlogs.DistributedLogsQueryParameters;
import io.stackgres.apiweb.distributedlogs.FullTextSearchQuery;
import io.stackgres.apiweb.distributedlogs.ImmutableDistributedLogsQueryParameters;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.CustomResourceFinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgclusters")
@RequestScoped
@Authenticated
public class NamespacedClusterLogsResource {

  private final CustomResourceFinder<ClusterDto> clusterFinder;
  private final DistributedLogsFetcher distributedLogsFetcher;

  @Inject
  public NamespacedClusterLogsResource(CustomResourceFinder<ClusterDto> clusterFinder,
      DistributedLogsFetcher distributedLogsFetcher) {
    this.clusterFinder = clusterFinder;
    this.distributedLogsFetcher = distributedLogsFetcher;
  }

  /**
   * Query distributed logs and return a list of {@code ClusterLogEntry}.
   */
  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ClusterLogEntryDto.class)))})
      })
  @CommonApiResponses
  @GET
  @Path("{name}/logs")
  public List<ClusterLogEntryDto> logs(
      @PathParam("namespace") String namespace,
      @PathParam("name") String name,
      @QueryParam("records") Integer records,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @QueryParam("sort") String sort,
      @QueryParam("text") String text,
      @QueryParam("logType") List<String> logType,
      @QueryParam("podName") List<String> podName,
      @QueryParam("role") List<String> role,
      @QueryParam("errorLevel") List<String> errorLevel,
      @QueryParam("userName") List<String> userName,
      @QueryParam("databaseName") List<String> databaseName,
      @QueryParam("fromInclusive") Boolean fromInclusive) {
    final ClusterDto cluster = clusterFinder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);

    final int calculatedRecords = records != null ? records : 50;

    if (calculatedRecords <= 0) {
      throw new BadRequestException("records should be a positive number");
    }

    final Optional<Tuple2<Instant, Integer>> fromTuple;
    final Optional<Tuple2<Instant, Integer>> toTuple;

    if (Optional.ofNullable(cluster.getSpec())
        .map(ClusterSpec::getDistributedLogs)
        .map(ClusterDistributedLogs::getSgDistributedLogs).isEmpty()) {
      throw new BadRequestException(
          "Distributed logs are not configured for specified cluster");
    }

    final var filters = ImmutableMap.<String, List<String>>builder();
    addFilter("logType", logType, filters);
    addFilter("podName", podName, filters);
    addFilter("role", role, filters);
    addFilter("errorLevel", errorLevel, filters);
    addFilter("userName", userName, filters);
    addFilter("databaseName", databaseName, filters);

    try {
      fromTuple = Optional.ofNullable(from)
          .map(s -> s.split(","))
          .map(ss -> Tuple.tuple(ss[0],
              ss.length > 1 ? ss[1] : String.valueOf(Integer.valueOf(0))))
          .map(t -> t.map1(Instant::parse))
          .map(t -> t.map2(Integer::valueOf));
    } catch (Exception ex) {
      throw new BadRequestException("from should be a timestamp"
          + " or a timestamp and an index separated by character ','", ex);
    }

    try {
      toTuple = Optional.ofNullable(to)
          .map(s -> s.split(","))
          .map(ss -> Tuple.tuple(ss[0],
              ss.length > 1 ? ss[1] : String.valueOf(Integer.MAX_VALUE)))
          .map(t -> t.map1(Instant::parse))
          .map(t -> t.map2(Integer::valueOf));
    } catch (Exception ex) {
      throw new BadRequestException("to should be a timestamp"
          + " or a timestamp and an index separated by character ','", ex);
    }

    if (sort != null && !sort.equals("asc") && !sort.equals("desc")) {
      throw new BadRequestException("sort only accept asc or desc values");
    }

    DistributedLogsQueryParameters logs = ImmutableDistributedLogsQueryParameters.builder()
        .cluster(cluster)
        .records(calculatedRecords)
        .fromTimeAndIndex(fromTuple)
        .toTimeAndIndex(toTuple)
        .filters(filters.build())
        .isSortAsc(Objects.equals("asc", sort))
        .fullTextSearchQuery(Optional.ofNullable(text)
            .map(FullTextSearchQuery::new))
        .isFromInclusive(fromInclusive != null && fromInclusive)
        .build();

    return distributedLogsFetcher.logs(logs);
  }

  private void addFilter(String key, List<String> values,
      final Builder<String, List<String>> filters) {
    if (values != null && !values.isEmpty()) {
      filters.put(key, values.stream()
          .filter(value -> !value.isEmpty())
          .toList());
    }
  }

}
