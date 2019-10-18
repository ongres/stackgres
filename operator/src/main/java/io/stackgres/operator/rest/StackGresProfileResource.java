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
import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.customresource.sgprofile.StackGresProfileList;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.KubernetesClientFactory;

@Path("/stackgres/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresProfileResource {

  @Inject
  KubernetesClientFactory kubeClient;

  /**
   * Return the list of {@code StackGresProfile}.
   */
  @GET
  public List<StackGresProfile> list() {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME)
          .map(crd -> client.customResources(crd,
              StackGresProfile.class,
              StackGresProfileList.class,
              StackGresProfileDoneable.class)
              .inAnyNamespace()
              .list()
              .getItems())
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
    }
  }

  /**
   * Return a {@code StackGresProfile}.
   */
  @Path("/{namespace}/{name}")
  @GET
  public StackGresProfile get(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME)
          .map(crd -> Optional.ofNullable(client.customResources(crd,
              StackGresProfile.class,
              StackGresProfileList.class,
              StackGresProfileDoneable.class)
              .inNamespace(namespace)
              .withName(name)
              .get())
              .orElseThrow(() -> new NotFoundException()))
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
    }
  }

  /**
   * Create a {@code StackGresProfile}.
   */
  @POST
  public void create(StackGresProfile cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresProfileDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresProfile.class,
          StackGresProfileList.class,
          StackGresProfileDoneable.class)
        .create(cluster);
    }
  }

  /**
   * Delete a {@code StackGresProfile}.
   */
  @DELETE
  public void delete(StackGresProfile cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresProfileDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresProfile.class,
          StackGresProfileList.class,
          StackGresProfileDoneable.class)
        .delete(cluster);
    }
  }

  /**
   * Create or update a {@code StackGresProfile}.
   */
  @PUT
  public void update(StackGresProfile cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresProfileDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresProfile.class,
          StackGresProfileList.class,
          StackGresProfileDoneable.class)
        .createOrReplace(cluster);
    }
  }
}
