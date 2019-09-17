/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtils {

  public static final String APP_KEY = "app";
  public static final String APP_NAME = "StackGres";
  public static final String CLUSTER_NAME_KEY = "cluster-name";

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtils.class);

  private ResourceUtils() {
    throw new AssertionError("No instances for you!");
  }

  /**
   * Filter metadata of resources to find if the name match in the provided list.
   *
   * @param list resources with metadata to filter
   * @param name to check for match in the list
   * @return true if the name exists in the list
   */
  public static boolean exists(List<? extends HasMetadata> list, String name) {
    return list.stream()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .anyMatch(name::equals);
  }

  /**
   * ImmutableMap of default labels used as selectors in K8s resources.
   */
  public static Map<String, String> defaultLabels(String clusterName) {
    return ImmutableMap.of(APP_KEY, APP_NAME, CLUSTER_NAME_KEY, clusterName);
  }

  /**
   * Get a custom resource definition from Kubernetes.
   *
   * @param client Kubernetes client to call the API.
   * @param crdName Name of the CDR to lookup.
   * @return the CustomResourceDefinition model.
   */
  public static Optional<CustomResourceDefinition> getCustomResource(KubernetesClient client,
      String crdName) {
    return Optional.ofNullable(client.customResourceDefinitions().withName(crdName).get());
  }

  /**
   * Log in debug the YAML of kubernetes resources passed as argument.
   *
   * @param resource KubernetesResource that has metadata
   */
  public static void logAsYaml(HasMetadata resource) {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{}: {}", resource.getClass().getSimpleName(),
            SerializationUtils.dumpWithoutRuntimeStateAsYaml(resource));
      }
    } catch (JsonProcessingException e) {
      LOGGER.debug("Error dump as Yaml:", e);
    }
  }

}
