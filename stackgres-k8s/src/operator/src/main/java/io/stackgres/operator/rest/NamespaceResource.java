/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

@Path("/stackgres/namespaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NamespaceResource {

  private final KubernetesClientFactory clientFactory;

  @Inject
  public NamespaceResource(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @GET
  public List<String> get() {
    try (KubernetesClient client = clientFactory.create()) {
      return client.namespaces().list().getItems()
          .stream()
          .map(namespace -> namespace.getMetadata().getName())
          .collect(ImmutableList.toImmutableList());
    }
  }

}
