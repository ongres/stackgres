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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.config.WebApiContext;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.common.resource.ResourceScanner;

@Path("/stackgres/namespace")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NamespaceResource {

  private final ResourceScanner<Namespace> namespaceScanner;

  private final String restApiNamespace;

  @Inject
  public NamespaceResource(ResourceScanner<Namespace> namespaceScanner,
      WebApiContext webApiContext) {
    super();
    this.namespaceScanner = namespaceScanner;
    this.restApiNamespace = webApiContext.get(WebApiProperty.RESTAPI_NAMESPACE);
  }

  @GET
  @Authenticated
  public List<String> get() {
    return namespaceScanner.findResources().stream()
        .map(namespace -> namespace.getMetadata().getName())
        .filter(namespace -> !namespace.startsWith("kube-"))
        .filter(namespace -> !namespace.equals(restApiNamespace))
        .collect(ImmutableList.toImmutableList());
  }

}
