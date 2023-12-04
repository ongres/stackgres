/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgpgconfigs")
@RequestScoped
@Authenticated
public class NamespacedPostgresConfigResource
    extends AbstractNamespacedRestServiceDependency<PostgresConfigDto, StackGresPostgresConfig> {

  @Override
  public boolean belongsToCluster(StackGresPostgresConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfigurations().getSgPostgresConfig(),
            resource.getMetadata().getName());
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostgresConfigDto.class))})
  @Override
  public PostgresConfigDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
