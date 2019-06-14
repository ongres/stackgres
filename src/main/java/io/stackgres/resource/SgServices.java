/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgServices {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgServices.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  @NonNull
  String namespace;

  @Inject
  @NonNull
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public @NonNull Service create(@NonNull String serviceName, @NonNull Integer port) {
    LOGGER.debug("Creating service name: {}", serviceName);

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "stackgres");
    labels.put("stackgres-cluster", serviceName);

    ServicePort ports = new ServicePortBuilder()
        .withProtocol("TCP")
        .withPort(port)
        .build();

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
          .withClusterIP("None")
          .endSpec()
          .build();

      LOGGER.debug("Creating service: {}", serviceName);

      client.services().inNamespace(namespace).createOrReplace(service);

      ServiceList listServices = client.services().inNamespace(namespace).list();
      for (Service item : listServices.getItems()) {
        LOGGER.debug(item.getMetadata().getName());
        if (item.getMetadata().getName().equals(serviceName)) {
          service = item;
        }
      }

      return service;
    }
  }

}
