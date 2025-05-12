/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgpoolconfig;

import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractNamespacedRestServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgpoolconfigs")
@RequestScoped
@Authenticated
@Tag(name = "sgpoolconfig")
@APIResponse(responseCode = "400", description = "Bad Request",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "401", description = "Unauthorized",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "403", description = "Forbidden",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "500", description = "Internal Server Error",
    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class))})
public class NamespacedConnectionPoolingConfigResource
    extends AbstractNamespacedRestServiceDependency<PoolingConfigDto, StackGresPoolingConfig> {

  @Override
  public boolean belongsToCluster(StackGresPoolingConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfigurations().getSgPoolingConfig(),
            resource.getMetadata().getName());
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PoolingConfigDto.class))})
  @Operation(summary = "Get a sgpoolconfig", description = """
      Get a sgpoolconfig.

      ### RBAC permissions required

      * sgpoolconfig get
      """)
  @Override
  public PoolingConfigDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
