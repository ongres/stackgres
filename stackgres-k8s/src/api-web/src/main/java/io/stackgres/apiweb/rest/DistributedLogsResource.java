/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("")
@RequestScoped
@Authenticated
public class DistributedLogsResource
    extends AbstractDependencyRestService<DistributedLogsDto, StackGresDistributedLogs> {

  @Override
  public boolean belongsToCluster(StackGresDistributedLogs resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getDistributedLogs),
            Optional.of(resource.getMetadata().getName()));
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = DistributedLogsDto.class))) })
      })
  @Override
  @Path("sgdistributedlogs")
  public List<DistributedLogsDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DistributedLogsDto.class)) })
      })
  @Override
  @Path("{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgdistributedlogs/{name}")
  public DistributedLogsDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgdistributedlogs")
  public void create(DistributedLogsDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgdistributedlogs")
  public void delete(DistributedLogsDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgdistributedlogs")
  public void update(DistributedLogsDto resource) {
    super.update(resource);
  }

}
