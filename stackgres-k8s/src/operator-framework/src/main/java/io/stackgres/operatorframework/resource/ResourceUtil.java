/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtil.class);
  private static final String INDEX_PATTERN = "^.*-([0-9]+)$";

  private ResourceUtil() {}

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

  public static String resourceName(String name) {
    Preconditions.checkArgument(name.length() <= 253);
    return name;
  }

  public static String volumeName(String name) {
    Preconditions.checkArgument(name.length() <= 63);
    return name;
  }

  public static String cutVolumeName(String name) {
    if (name.length() <= 63) {
      return name;
    }
    String cutName = name.substring(0, 63);
    if (cutName.matches("^.*[^A-Za-z0-9]$")) {
      cutName = cutName.substring(0, 62) + 'x';
    }
    return cutName;
  }

  public static String containerName(String name) {
    Preconditions.checkArgument(name.length() <= 63);
    return name;
  }

  public static String labelKey(String name) {
    Preconditions.checkArgument(name.length() <= 63);
    return name;
  }

  public static String labelValue(String name) {
    Preconditions.checkArgument(name.length() <= 63);
    return name;
  }

  public static String getIndexPattern() {
    return INDEX_PATTERN;
  }

  public static String getNameWithIndexPattern(String name) {
    return "^" + Pattern.quote(name) + "-([0-9]+)$";
  }

  public static String getNameWithHashPattern(String name) {
    return "^" + Pattern.quote(name) + "-([a-z0-9]+){10}-([a-z0-9]+){5}$";
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
    return Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
        .withName(crdName).get());
  }

  /**
   * Log in debug the YAML of kubernetes resources passed as argument.
   *
   * @param resource KubernetesResource that has metadata
   */
  public static void logAsYaml(HasMetadata resource) {
    if (LOGGER.isDebugEnabled()) {
      try {
        LOGGER.debug("{}: {}", resource.getClass().getSimpleName(),
            SerializationUtils.dumpWithoutRuntimeStateAsYaml(resource));
      } catch (JsonProcessingException e) {
        LOGGER.debug("Error dump as Yaml:", e);
      }
    }
  }

  /**
   * Get the object reference of any resource.
   */
  public static ObjectReference getObjectReference(HasMetadata resource) {
    return new ObjectReferenceBuilder()
        .withApiVersion(resource.getApiVersion())
        .withKind(resource.getKind())
        .withNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .withResourceVersion(resource.getMetadata().getResourceVersion())
        .withUid(resource.getMetadata().getUid())
        .build();
  }

  /**
   * Get the owner reference of any resource.
   */
  public static OwnerReference getOwnerReference(HasMetadata resource) {
    return new OwnerReferenceBuilder()
        .withApiVersion(resource.getApiVersion())
        .withKind(resource.getKind())
        .withName(resource.getMetadata().getName())
        .withUid(resource.getMetadata().getUid())
        .withController(true)
        .build();
  }

  public static String encodeSecret(String string) {
    return Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
  }

  public static String dencodeSecret(String string) {
    return new String(Base64.getDecoder().decode(string.getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);
  }

  public static String generateRandom(int length) {
    int leftLimit = 48; // numeral '0'
    int rightLimit = 122; // letter 'z'
    Random random = new SecureRandom();

    return random.ints(leftLimit, rightLimit + 1)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

  }

}
