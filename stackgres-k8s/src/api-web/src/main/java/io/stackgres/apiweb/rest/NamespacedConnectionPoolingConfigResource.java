/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgpoolconfigs")
@RequestScoped
@Authenticated
public class NamespacedConnectionPoolingConfigResource
    extends AbstractNamespacedRestServiceDependency<PoolingConfigDto, StackGresPoolingConfig> {

  @Override
  public boolean belongsToCluster(StackGresPoolingConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfiguration().getConnectionPoolingConfig(),
            resource.getMetadata().getName());
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = PoolingConfigDto.class))})
      })
  @Override
  public PoolingConfigDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
