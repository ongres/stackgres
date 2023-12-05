/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sginstanceprofile;

import java.util.Objects;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.rest.AbstractNamespacedRestServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ProfileDto.class))})
  @Tag(name = "sginstanceprofile")
  @Operation(summary = "Get a sginstanceprofile", description = """
      Get a sginstanceprofile.

      ### RBAC permissions required

      * sginstanceprofiles get
      """)
  @Override
  public ProfileDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
