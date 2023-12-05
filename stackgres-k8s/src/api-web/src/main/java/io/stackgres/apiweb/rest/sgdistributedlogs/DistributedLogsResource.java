/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgdistributedlogs;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.rest.AbstractCustomResourceServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgdistributedlogs")
@RequestScoped
@Authenticated
public class DistributedLogsResource
    extends AbstractCustomResourceServiceDependency<DistributedLogsDto, StackGresDistributedLogs> {

  @Override
  public boolean belongsToCluster(StackGresDistributedLogs resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getSgDistributedLogs),
            Optional.of(resource.getMetadata().getName()));
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = DistributedLogsDto.class))})
  @Tag(name = "sgdistributedlogs")
  @Operation(summary = "List sgdistributedlogs", description = """
      List sgdistributedlogs.

      ### RBAC permissions required

      * sgdistributedlogs list
      """)
  @Override
  public List<DistributedLogsDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = DistributedLogsDto.class))})
  @Tag(name = "sgdistributedlogs")
  @Operation(summary = "Create a sgdistributedlogs", description = """
      Create a sgdistributedlogs.

      ### RBAC permissions required

      * sgdistributedlogs create
      """)
  @Override
  public DistributedLogsDto create(DistributedLogsDto resource, @Nullable Boolean dryRun) {
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Tag(name = "sgdistributedlogs")
  @Operation(summary = "Delete a sgdistributedlogs", description = """
      Delete a sgdistributedlogs.

      ### RBAC permissions required

      * sgdistributedlogs delete
      """)
  @Override
  public void delete(DistributedLogsDto resource, @Nullable Boolean dryRun) {
    super.delete(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = DistributedLogsDto.class))})
  @Tag(name = "sgdistributedlogs")
  @Operation(summary = "Update a sgdistributedlogs", description = """
      Update a sgdistributedlogs.

      ### RBAC permissions required

      * sgdistributedlogs patch
      """)
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
