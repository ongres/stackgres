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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/v1alpha1/pod")
public class Pods {

  private static final Logger log = LoggerFactory.getLogger(Pods.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create a new Pod with the specification.
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Pod createPod(@FormParam("podName") String podName, @FormParam("podCpu") String podCpu,
      @FormParam("podMem") String podMem) throws IOException {

    log.debug("Creating pod name: {}", podName);

    Map<String, Quantity> limits = new HashMap<>(2);
    if (!"".equals(podCpu)) {
      limits.put("cpu", new Quantity(podCpu));
    }
    if (!"".equals(podMem)) {
      limits.put("memory", new Quantity(podMem));
    }

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Pod pod = new PodBuilder()
          .withKind("Pod")
          .withNewMetadata()
          .withName(podName)
          .endMetadata()
          .withNewSpec()
          .addNewContainer()
          .withName("postgres")
          .withImage("postgres:11")
          .withNewResources()
          .withLimits(limits)
          .endResources()
          .endContainer()
          .endSpec()
          .build();

      log.debug("Creating pod: {}", podName);

      client.pods().inNamespace(namespace).createOrReplace(pod);


      PodList listPod = client.pods().inNamespace(namespace).list();
      for (Pod item : listPod.getItems()) {
        log.debug(item.getMetadata().getName());
        pod = item.getMetadata().getName().equals(podName) ? item : null;
      }

      return pod;
    }
  }

  /**
   * List all pods in namespace.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public PodList list() {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return client.pods().inNamespace(namespace).list();
    }
  }

}
