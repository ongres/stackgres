/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;

@Path("/api/v1alpha1/deployment")
public class Deployments {

  private static final Logger log = LoggerFactory.getLogger(Deployments.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Deployment create(@FormParam("deployName") String deployName,
      @FormParam("replicas") Integer replicas, @FormParam("podCpu") String podCpu,
      @FormParam("podMem") String podMem) throws IOException {

    log.debug("Creating deploy name: {}", deployName);
    log.debug("Replicas: {}", replicas);
    log.debug("podCpu: {}", podCpu);
    log.debug("podMem: {}", podMem);

    Map<String, String> labels = new HashMap<>();
    labels.put("app", namespace);

    Map<String, Quantity> limits = new HashMap<>(2);
    if (!"".equals(podCpu)) {
      limits.put("cpu", new Quantity(podCpu));
    }
    if (!"".equals(podMem)) {
      limits.put("memory", new Quantity(podMem));
    }

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Deployment deploy = new DeploymentBuilder()
          .withKind("Deployment")
          .withNewMetadata()
          .withName(deployName)
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withReplicas(replicas)
          .withSelector(new LabelSelectorBuilder()
              .addToMatchLabels(labels)
              .build())
          .withTemplate(new PodTemplateSpecBuilder()
              .withMetadata(new ObjectMetaBuilder()
                  .addToLabels(labels)
                  .build())
              .withNewSpec()
              .addNewContainer()
              .withName("sg-postgres")
              .withImage("postgres:11")
              .withResources(new ResourceRequirementsBuilder().addToLimits(limits).build())
              .withPorts(new ContainerPortBuilder().withContainerPort(5432).build())
              .endContainer()
              .endSpec()
              .build())
          .endSpec()
          .build();

      log.debug("Creating or replacing deployment: {}", deployName);

      client.apps().deployments().inNamespace(namespace).createOrReplace(deploy);
      return deploy;
    }
  }

}
