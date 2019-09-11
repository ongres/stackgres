/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.common.sgcluster.StackGresCluster;
import io.stackgres.common.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.sgcluster.StackGresClusterList;
import io.stackgres.operator.app.KubernetesClientFactory;

@Path("/clusters")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresClusterResource {

  @Inject
  KubernetesClientFactory kubeClient;

  @GET
  public List<StackGresCluster> list() {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtils.getCustomResource(client, StackGresClusterDefinition.NAME)
          .map(crd -> client.customResources(crd,
              StackGresCluster.class,
              StackGresClusterList.class,
              StackGresClusterDoneable.class)
              .list()
              .getItems())
          .orElseGet(() -> ImmutableList.of());
    }
  }

  @POST
  public void create(StackGresCluster cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresClusterDefinition.NAME)
        .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
            + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
      .create(cluster);
    }
  }

  @DELETE
  public void delete(StackGresCluster cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresClusterDefinition.NAME)
        .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
            + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
      .delete(cluster);
    }
  }

  @PUT
  public void update(StackGresCluster cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresClusterDefinition.NAME)
        .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
            + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
      .createOrReplace(cluster);
    }
  }
}