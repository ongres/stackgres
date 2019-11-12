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
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.operator.resource.ResourceUtil;

@Path("/stackgres/backupconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresBackupConfigResource {

  @Inject
  KubernetesClientFactory kubeClient;

  /**
   * Return the list of {@code StackGresBackupConfig}.
   */
  @GET
  public List<StackGresBackupConfig> list() {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtil.getCustomResource(client, StackGresBackupConfigDefinition.NAME)
          .map(crd -> client.customResources(crd,
              StackGresBackupConfig.class,
              StackGresBackupConfigList.class,
              StackGresBackupConfigDoneable.class)
              .inAnyNamespace()
              .list()
              .getItems())
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupConfigDefinition.NAME + " not found."));
    }
  }

  /**
   * Return a {@code StackGresBackupConfig}.
   */
  @Path("/{namespace}/{name}")
  @GET
  public StackGresBackupConfig get(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtil.getCustomResource(client, StackGresBackupConfigDefinition.NAME)
          .map(crd -> Optional.ofNullable(client.customResources(crd,
              StackGresBackupConfig.class,
              StackGresBackupConfigList.class,
              StackGresBackupConfigDoneable.class)
              .inNamespace(namespace)
              .withName(name)
              .get())
              .orElseThrow(() -> new NotFoundException()))
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupConfigDefinition.NAME + " not found."));
    }
  }

  /**
   * Create a {@code StackGresBackupConfig}.
   */
  @POST
  public void create(StackGresBackupConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackupConfig.class,
          StackGresBackupConfigList.class,
          StackGresBackupConfigDoneable.class)
        .create(cluster);
    }
  }

  /**
   * Delete a {@code StackGresBackupConfig}.
   */
  @DELETE
  public void delete(StackGresBackupConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackupConfig.class,
          StackGresBackupConfigList.class,
          StackGresBackupConfigDoneable.class)
        .delete(cluster);
    }
  }

  /**
   * Create or update a {@code StackGresBackupConfig}.
   */
  @PUT
  public void update(StackGresBackupConfig cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackupConfig.class,
          StackGresBackupConfigList.class,
          StackGresBackupConfigDoneable.class)
        .createOrReplace(cluster);
    }
  }
}
