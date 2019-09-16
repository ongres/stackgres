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
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;

@Path("/connpoolconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresConnectionPoolingConfigResource {

  @Inject
  KubernetesClientFactory kubeClient;

  /**
   * Return the list of {@code StackGresPgbouncerConfig}.
   */
  @GET
  public List<StackGresPgbouncerConfig> list() {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtils.getCustomResource(client, StackGresPgbouncerConfigDefinition.NAME)
          .map(crd -> client.customResources(crd,
              StackGresPgbouncerConfig.class,
              StackGresPgbouncerConfigList.class,
              StackGresPgbouncerConfigDoneable.class)
              .list()
              .getItems())
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
    }
  }

  /**
   * Return a {@code StackGresPgbouncerConfig}.
   */
  @Path("/{namespace}/{name}")
  @GET
  public StackGresPgbouncerConfig get(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtils.getCustomResource(client, StackGresPgbouncerConfigDefinition.NAME)
          .map(crd -> Optional.ofNullable(client.customResources(crd,
              StackGresPgbouncerConfig.class,
              StackGresPgbouncerConfigList.class,
              StackGresPgbouncerConfigDoneable.class)
              .inNamespace(namespace)
              .withName(name)
              .get())
              .orElseThrow(() -> new NotFoundException()))
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
    }
  }

  /**
   * Create a {@code StackGresPgbouncerConfig}.
   */
  @POST
  public void create(StackGresPgbouncerConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresPgbouncerConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
            + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPgbouncerConfig.class,
          StackGresPgbouncerConfigList.class,
          StackGresPgbouncerConfigDoneable.class)
        .create(cluster);
    }
  }

  /**
   * Delete a {@code StackGresPgbouncerConfig}.
   */
  @DELETE
  public void delete(StackGresPgbouncerConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresPgbouncerConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPgbouncerConfig.class,
          StackGresPgbouncerConfigList.class,
          StackGresPgbouncerConfigDoneable.class)
        .delete(cluster);
    }
  }

  /**
   * Create or update a {@code StackGresPgbouncerConfig}.
   */
  @PUT
  public void update(StackGresPgbouncerConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtils.getCustomResource(
          client, StackGresPgbouncerConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPgbouncerConfig.class,
          StackGresPgbouncerConfigList.class,
          StackGresPgbouncerConfigDoneable.class)
        .createOrReplace(cluster);
    }
  }
}
