/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgdistributedlogs")
@RequestScoped
@Authenticated
public class NamespacedDistributedLogsResource
    extends AbstractNamespacedRestServiceDependency<DistributedLogsDto, StackGresDistributedLogs> {

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
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = DistributedLogsDto.class))})
      })
  @Override
  public DistributedLogsDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
