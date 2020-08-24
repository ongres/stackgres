/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.secret.SecretDto;
import io.stackgres.common.resource.ResourceScanner;

@Path("/stackgres/secrets")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecretResource {

  private final ResourceScanner<SecretDto> scanner;

  @Inject
  public SecretResource(ResourceScanner<SecretDto> scanner) {
    this.scanner = scanner;
  }

  @Path("/{namespace}")
  @GET
  @Authenticated
  public List<SecretDto> list(@PathParam("namespace") String namespace) {
    return scanner.findResourcesInNamespace(namespace);
  }
}
