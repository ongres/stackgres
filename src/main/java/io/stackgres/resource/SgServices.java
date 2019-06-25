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
  public @NonNull Service create(@NonNull String serviceName, @NonNull Integer postgresPort) {
    LOGGER.debug("Creating service name: {}", serviceName);

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "StackGres");
    labels.put("cluster-name", serviceName);

    labels.put("role", "master");
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Service service1 = new ServiceBuilder()
          .withNewMetadata()
          .withName(serviceName + "-primary")
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withSelector(labels)
          .withPorts(
              new ServicePortBuilder()
                  .withProtocol("TCP")
                  .withPort(postgresPort)
                  .build())
          .withType("ClusterIP")
          .endSpec()
          .build();

      LOGGER.debug("Creating service ReadWrite: {}-{}", serviceName, "primary");
      client.services().inNamespace(namespace).createOrReplace(service1);

      labels.put("role", "replica");
      Service service2 = new ServiceBuilder()
          .withNewMetadata()
          .withName(serviceName + "-replica")
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withSelector(labels)
          .withPorts(
              new ServicePortBuilder()
                  .withProtocol("TCP")
                  .withPort(postgresPort)
                  .build())
          .withType("ClusterIP")
          .endSpec()
          .build();

      LOGGER.debug("Creating service ReadOnly: {}-{}", serviceName, "replica");
      client.services().inNamespace(namespace).createOrReplace(service2);

      ServiceList listServices = client.services().inNamespace(namespace).list();
      for (Service item : listServices.getItems()) {
        LOGGER.debug(item.getMetadata().getName());
        if (item.getMetadata().getName().equals(serviceName + "-primary")) {
          service1 = item;
        }
      }

      return service1;
    }
  }

}
