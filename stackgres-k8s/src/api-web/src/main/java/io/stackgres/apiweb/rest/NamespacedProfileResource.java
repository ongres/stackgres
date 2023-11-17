/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sginstanceprofiles")
@RequestScoped
@Authenticated
public class NamespacedProfileResource
    extends AbstractNamespacedRestServiceDependency<ProfileDto, StackGresProfile> {

  @Override
  public boolean belongsToCluster(StackGresProfile resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getSgInstanceProfile(),
            resource.getMetadata().getName());
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ProfileDto.class))})
      })
  @Override
  public ProfileDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
