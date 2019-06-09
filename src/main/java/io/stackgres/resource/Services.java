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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;

@Path("/api/v1alpha1/service")
public class Services {

  private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Service create(@FormParam("serviceName") String serviceName,
      @FormParam("port") Integer port) throws IOException {
    LOGGER.debug("Creating service name: {}", serviceName);

    Map<String, String> labels = new HashMap<>();
    labels.put("app", namespace);

    ServicePort ports = new ServicePort();
    ports.setProtocol("TCP");
    ports.setPort(port);

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Service service = new ServiceBuilder()
          .withKind("Service")
          .withNewMetadata()
          .withName(serviceName)
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withSelector(labels)
          .withPorts(ports)
          .endSpec()
          .build();

      LOGGER.debug("Creating service: {}", serviceName);

      client.services().inNamespace(namespace).createOrReplace(service);


      ServiceList listServices = client.services().inNamespace(namespace).list();
      for (Service item : listServices.getItems()) {
        LOGGER.debug(item.getMetadata().getName());
        service = item.getMetadata().getName().equals(serviceName) ? item : null;
      }


      return service;
    }
  }

}
