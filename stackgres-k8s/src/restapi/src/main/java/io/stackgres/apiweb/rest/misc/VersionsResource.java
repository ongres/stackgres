/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("version")
@RequestScoped
@Authenticated
@Tag(name = "misc")
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
public class VersionsResource {

  private final StackGresContext context;

  @Inject
  public VersionsResource(StackGresContext context) {
    this.context = context;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(type = SchemaType.OBJECT))})
  @Operation(summary = "List postgres versions", description = """
      List of the supported postgres versions.

      Allowed values that can be used in the SGCluster postgresVersion definition.

      ### RBAC permissions required

      None
      """)
  @GET
  @Path("postgresql")
  public Map<String, List<String>> supportedPostgresVersions(
      @QueryParam("flavor") String flavor,
      @QueryParam("sgVersion") String sgVersion) {
    final StackGresComponent flavorComponent = StackGresUtil.getPostgresFlavorComponent(flavor);
    return Map.of(
        "postgresql",
        Optional.ofNullable(sgVersion)
        .map(version -> Stream.of(StackGresVersion.values())
            .filter(foundVersion -> version.equals(foundVersion.getVersion()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid sgVersion " + version)))
        .map(flavorComponent::getOrThrow)
        .orElseGet(() -> flavorComponent.get(registryCluster(flavor)))
        .streamOrderedVersions(context).toList());
  }

  /**
   * A SGCluster of the flavor with the images registry enabled, used to list the versions served
   * by the default docir repository when no operator version is specified.
   */
  private StackGresCluster registryCluster(String flavor) {
    final StackGresCluster cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getSpec().setPostgres(new StackGresClusterPostgres());
    cluster.getSpec().getPostgres().setFlavor(flavor);
    cluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
    cluster.getSpec().getConfigurations().setRegistry(new StackGresClusterRegistry());
    cluster.getSpec().getConfigurations().getRegistry().setEnabled(true);
    return cluster;
  }

}
