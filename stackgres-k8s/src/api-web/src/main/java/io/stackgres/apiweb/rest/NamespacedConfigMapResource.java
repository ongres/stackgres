/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.configmap.ConfigMapDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/configmaps")
@RequestScoped
@Authenticated
public class NamespacedConfigMapResource {

  private ResourceScanner<ConfigMapDto> scanner;

  @Inject
  public NamespacedConfigMapResource(ResourceScanner<ConfigMapDto> scanner) {
    this.scanner = scanner;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ConfigMapDto.class))})
  @CommonApiResponses
  @GET
  public List<ConfigMapDto> list(@PathParam("namespace") String namespace) {
    return scanner.findResourcesInNamespace(namespace);
  }

}
