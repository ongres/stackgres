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
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import io.stackgres.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgServices {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgServices.class);

  private static final String READ_WRITE_SERVICE = "-primary";
  private static final String READ_ONLY_SERVICE = "-replica";
  private static final String CONFIG_SERVICE = "-config";

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public Service create(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return createServices(client, resource);
    }
  }

  private Service createConfigService(String serviceName, Map<String, String> labels) {
    LOGGER.debug("Creating service: {}", serviceName);
    return new ServiceBuilder()
        .withNewMetadata()
        .withName(serviceName)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
        .endSpec()
        .build();
  }

  private Service createService(String serviceName, String role, Map<String, String> labels) {
    final Map<String, String> labelsRole = new HashMap<>(labels);
    labelsRole.put("role", role); // role is set by Patroni

    LOGGER.debug("Creating service: {}", serviceName);
    return new ServiceBuilder()
        .withNewMetadata()
        .withName(serviceName)
        .withLabels(labelsRole)
        .endMetadata()
        .withNewSpec()
        .withSelector(labelsRole)
        .withPorts(new ServicePortBuilder()
            .withProtocol("TCP")
            .withPort(5432)
            .build())
        .withType("NodePort")
        .endSpec()
        .build();
  }

  private Service createServices(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtils.defaultLabels(name);

    Service config = createConfigService(name + CONFIG_SERVICE, labels);
    config = client.services().inNamespace(namespace).createOrReplace(config);

    Service primary = createService(name + READ_WRITE_SERVICE, "master", labels);
    client.services().inNamespace(namespace).createOrReplace(primary);

    Service replicas = createService(name + READ_ONLY_SERVICE, "replica", labels);
    client.services().inNamespace(namespace).createOrReplace(replicas);

    return config;
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
    final String namespace = resource.getMetadata().getNamespace();
    deleteService(client, name + READ_WRITE_SERVICE, namespace);
    deleteService(client, name + READ_ONLY_SERVICE, namespace);
    return deleteService(client, name + CONFIG_SERVICE, namespace);
  }

  private Service deleteService(KubernetesClient client, String name, String namespace) {
    Service srv = client.services().inNamespace(namespace).withName(name).get();
    if (srv != null) {
      client.services().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting Service: {}", name);
    }
    return srv;
  }

}
