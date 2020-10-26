/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("/stackgres/sginstanceprofile")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource
    extends AbstractDependencyRestService<ProfileDto, StackGresProfile> {

  @Inject
  public ProfileResource(
      CustomResourceScanner<StackGresProfile> scanner,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      DependencyResourceTransformer<ProfileDto, StackGresProfile> transformer) {
    super(scanner, finder, scheduler, clusterScanner, transformer);
  }

  public ProfileResource() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  public boolean belongsToCluster(StackGresProfile resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getResourceProfile(),
            resource.getMetadata().getName());
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ProfileDto.class))) })
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public List<ProfileDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ProfileDto.class)) })
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public ProfileDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void create(ProfileDto resource) {
    super.create(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void delete(ProfileDto resource) {
    super.delete(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @CommonApiResponses
  @Authenticated
  @Override
  public void update(ProfileDto resource) {
    super.update(resource);
  }

}
