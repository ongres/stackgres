/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Namespace;
import io.stackgres.operator.resource.ResourceScanner;

@Path("/stackgres/namespace")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NamespaceResource {

  private ResourceScanner<Namespace> namespaceScanner;

  @GET
  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  public List<String> get() {
    return namespaceScanner.findResources().stream()
        .map(namespace -> namespace.getMetadata().getName())
        .filter(namespace -> !namespace.startsWith("kube-"))
        .collect(ImmutableList.toImmutableList());
  }

  @Inject
  public void setNamespaceScanner(ResourceScanner<Namespace> namespaceScanner) {
    this.namespaceScanner = namespaceScanner;
  }

}
