/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@Path("sgdistributedlogs")
@RequestScoped
@Authenticated
public class DistributedLogsResource
    extends AbstractRestServiceDependency<DistributedLogsDto, StackGresDistributedLogs> {

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
  public List<DistributedLogsDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(DistributedLogsDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(DistributedLogsDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(DistributedLogsDto resource) {
    super.update(resource);
  }

  @Override
  protected void updateSpec(StackGresDistributedLogs resourceToUpdate,
      StackGresDistributedLogs resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
