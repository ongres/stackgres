/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.parameters.DefaultValues;
import io.stackgres.operator.patroni.PatroniConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgServices {

  public static final String READ_WRITE_SERVICE = "-primary";
  public static final String READ_ONLY_SERVICE = "-replica";
  public static final String CONFIG_SERVICE = "-config";

  private static final Logger LOGGER = LoggerFactory.getLogger(SgServices.class);

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

  private Endpoints createConfigEndpoint(String endpointName, Map<String, String> labels) {
    LOGGER.debug("Creating endpoint: {}", endpointName);
    PatroniConfig config = new PatroniConfig();
    config.setTtl(30);
    config.setLoopWait(10);
    config.setRetryTimeout(10);
    PatroniConfig.PostgreSql postgres = new PatroniConfig.PostgreSql();
    postgres.setUsePgRewind(true);
    postgres.setParameters(DefaultValues.getDefaultValues());
    config.setPostgresql(postgres);

    Map<String, String> annotations = new HashMap<>();
    try {
      ObjectMapper mapper = new ObjectMapper();
      annotations.put("config", mapper.writeValueAsString(config));
    } catch (JsonProcessingException ignore) {
      annotations.put("config", "");
    }

    return new EndpointsBuilder()
        .withNewMetadata()
        .withName(endpointName)
        .withLabels(labels)
        .withAnnotations(annotations)
        .endMetadata()
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
        .withType("LoadBalancer")
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

    createConfigurationEndpoint(client, name, namespace, labels);

    return config;
  }

  private void createConfigurationEndpoint(KubernetesClient client, final String name,
      final String namespace, final Map<String, String> labels) {
    Endpoints epConfig = client.endpoints().inNamespace(namespace)
        .withName(name + CONFIG_SERVICE).get();
    if (epConfig == null) {
      epConfig = createConfigEndpoint(name + CONFIG_SERVICE, labels);
      epConfig = client.endpoints().inNamespace(namespace).create(epConfig);
      LOGGER.debug("Creating Endpoint: {}", name);
      LOGGER.trace("Creating Endpoint: {}", epConfig);
    }
  }

  /**
   * Delete resource.
   */
  public void delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public void delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtils.defaultLabels(name);

    Boolean ep = client.endpoints().inNamespace(namespace)
        .withLabels(labels)
        .delete();
    LOGGER.debug("Deleting endpoints: {}, success: {}", name, ep);

    Boolean svc = client.services().inNamespace(namespace)
        .withLabels(labels)
        .delete();
    LOGGER.debug("Deleting services: {}, success: {}", name, svc);
  }

}
