/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("namespaces")
@RequestScoped
@Authenticated
public class NamespaceResource {

  private final ResourceScanner<Namespace> namespaceScanner;

  private final ResourceWriter<Namespace> namespaceWriter;

  @Inject
  public NamespaceResource(
      ResourceScanner<Namespace> namespaceScanner,
      ResourceWriter<Namespace> namespaceWriter) {
    this.namespaceScanner = namespaceScanner;
    this.namespaceWriter = namespaceWriter;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(type = "string"))) })
      })
  @CommonApiResponses
  @GET
  public List<String> get() {
    return namespaceScanner.findResources().stream()
        .map(namespace -> namespace.getMetadata().getName())
        .toList();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(type = "string"))) })
      })
  @CommonApiResponses
  @Path("{name}")
  @POST
  public void create(@PathParam("name") String name) {
    namespaceWriter.create(
        new NamespaceBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .build());
  }

}
