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
import io.stackgres.crd.sgcluster.StackGresCluster;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("initialization.fields.uninitialized")
@ApplicationScoped
public class SgServices {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgServices.class);

  private static final String READ_WRITE_SERVICE = "-primary";
  private static final String READ_ONLY_SERVICE = "-replica";
  private static final String CONFIG_SERVICE = "-config";

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  String namespace;

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public Service create(StackGresCluster sgcluster) {
    final String name = sgcluster.getMetadata().getName();

    Map<String, String> labels = new HashMap<>();
    labels.put("app", "StackGres");
    labels.put("cluster-name", name);

    labels.put("role", "master");
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      Service readWriteService = new ServiceBuilder()
          .withNewMetadata()
          .withName(name + READ_WRITE_SERVICE)
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withSelector(labels)
          .withPorts(
              new ServicePortBuilder()
                  .withProtocol("TCP")
                  .withPort(5432)
                  .build())
          .withType("ClusterIP")
          .endSpec()
          .build();

      LOGGER.debug("Creating service ReadWrite: {}{}", name, READ_WRITE_SERVICE);
      client.services().inNamespace(namespace).createOrReplace(readWriteService);
      LOGGER.trace("Service: {}", readWriteService);

      labels.put("role", "replica");
      Service readOnlyService = new ServiceBuilder()
          .withNewMetadata()
          .withName(name + READ_ONLY_SERVICE)
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withSelector(labels)
          .withPorts(
              new ServicePortBuilder()
                  .withProtocol("TCP")
                  .withPort(5432)
                  .build())
          .withType("ClusterIP")
          .endSpec()
          .build();

      LOGGER.debug("Creating service ReadOnly: {}{}", name, READ_ONLY_SERVICE);
      client.services().inNamespace(namespace).createOrReplace(readOnlyService);
      LOGGER.trace("Service: {}", readOnlyService);

      ServiceList listServices = client.services().inNamespace(namespace).list();
      for (Service item : listServices.getItems()) {
        if (item.getMetadata().getName().equals(name + READ_WRITE_SERVICE)) {
          readWriteService = item;
        }
      }

      return readWriteService;
    }
  }

  /**
   * Delete resource.
   */
  public Service delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public Service delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    deleteService(client, name + CONFIG_SERVICE);
    deleteService(client, name + READ_ONLY_SERVICE);
    return deleteService(client, name + READ_WRITE_SERVICE);
  }

  private Service deleteService(KubernetesClient client, String name) {
    Service srv = client.services().inNamespace(namespace).withName(name).get();
    if (srv != null) {
      client.services().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting Service: {}", name);
    }
    return srv;
  }

}
