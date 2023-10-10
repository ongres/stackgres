/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
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
            .map(StackGresClusterDistributedLogs::getSgDistributedLogs),
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
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DistributedLogsDto.class)) })
      })
  @Override
  public DistributedLogsDto create(DistributedLogsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(DistributedLogsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DistributedLogsDto.class)) })
      })
  @Override
  public DistributedLogsDto update(DistributedLogsDto resource, @Nullable Boolean dryRun) {
    return super.update(resource, dryRun);
  }

  @Override
  protected void updateSpec(StackGresDistributedLogs resourceToUpdate,
      StackGresDistributedLogs resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
