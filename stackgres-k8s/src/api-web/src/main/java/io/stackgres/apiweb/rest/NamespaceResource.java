/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.fabric8.kubernetes.api.model.Namespace;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.ResourceScanner;
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

  @Inject
  public NamespaceResource(ResourceScanner<Namespace> namespaceScanner) {
    super();
    this.namespaceScanner = namespaceScanner;
  }

  public NamespaceResource() {
    this.namespaceScanner = null;
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
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

}
