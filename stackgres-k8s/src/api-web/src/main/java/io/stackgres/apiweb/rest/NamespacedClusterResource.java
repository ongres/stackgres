/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgclusters")
@RequestScoped
@Authenticated
public class NamespacedClusterResource
    extends AbstractNamespacedRestService<ClusterDto, StackGresCluster> {

  private final ClusterResource clusterResource;

  private final CustomResourceFinder<ClusterDto> clusterFinder;

  @Inject
  public NamespacedClusterResource(CustomResourceFinder<ClusterDto> clusterFinder,
      ClusterResource clusterResource) {
    this.clusterResource = clusterResource;
    this.clusterFinder = clusterFinder;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ClusterDto.class))})
      })
  @Override
  public ClusterDto get(String namespace, String name) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(clusterResource::setScripts)
        .map(clusterResource::setConfigMaps)
        .map(clusterResource::setInfo)
        .orElseThrow(NotFoundException::new);
  }

}
