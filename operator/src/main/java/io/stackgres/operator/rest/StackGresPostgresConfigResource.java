/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresources.sgpgconfig.StackGresPostgresConfigList;

@Path("/pgconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresPostgresConfigResource {

  @Inject
  KubernetesClientFactory kubeClient;

  /**
   * Return the list of {@code StackGresPostgresConfig}.
   */
  @GET
  public List<StackGresPostgresConfig> list() {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtils.getCustomResource(client, StackGresPostgresConfigDefinition.NAME)
          .map(crd -> client.customResources(crd,
              StackGresPostgresConfig.class,
              StackGresPostgresConfigList.class,
              StackGresPostgresConfigDoneable.class)
              .list()
              .getItems())
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
    }
  }

  /**
   * Return a {@code StackGresPostgresConfig}.
   */
  @Path("/{namespace}/{name}")
  @GET
  public StackGresPostgresConfig get(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtils.getCustomResource(client, StackGresPostgresConfigDefinition.NAME)
          .map(crd -> Optional.ofNullable(client.customResources(crd,
              StackGresPostgresConfig.class,
              StackGresPostgresConfigList.class,
              StackGresPostgresConfigDoneable.class)
              .inNamespace(namespace)
              .withName(name)
              .get())
              .orElseThrow(() -> new NotFoundException()))
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
    }
  }

  /**
   * Create a {@code StackGresPostgresConfig}.
   */
  @POST
  public void create(StackGresPostgresConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresPostgresConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPostgresConfig.class,
          StackGresPostgresConfigList.class,
          StackGresPostgresConfigDoneable.class)
        .create(cluster);
    }
  }

  /**
   * Delete a {@code StackGresPostgresConfig}.
   */
  @DELETE
  public void delete(StackGresPostgresConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresPostgresConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPostgresConfig.class,
          StackGresPostgresConfigList.class,
          StackGresPostgresConfigDoneable.class)
        .delete(cluster);
    }
  }

  /**
   * Create or update a {@code StackGresPostgresConfig}.
   */
  @PUT
  public void update(StackGresPostgresConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresPostgresConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPostgresConfig.class,
          StackGresPostgresConfigList.class,
          StackGresPostgresConfigDoneable.class)
        .createOrReplace(cluster);
    }
  }
}
