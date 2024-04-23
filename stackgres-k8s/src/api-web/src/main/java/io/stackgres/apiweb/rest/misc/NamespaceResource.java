/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("namespaces")
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

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY))})
  @Operation(summary = "List namespaces", description = """
      List namespaces.

      ### RBAC permissions required

      * namespaces list
      """)
  @GET
  public List<String> get() {
    return namespaceScanner.findResources().stream()
        .map(namespace -> namespace.getMetadata().getName())
        .toList();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.STRING))})
  @Operation(summary = "Create namespaces", description = """
      Create namespaces.

      ### RBAC permissions required

      * namespaces create
      """)
  @Path("{name}")
  @POST
  public void create(@PathParam("name") String name, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    namespaceWriter.create(
        new NamespaceBuilder()
            .withNewMetadata()
            .withName(name)
            .endMetadata()
            .build(),
            Optional.ofNullable(dryRun).orElse(false));
  }

}
