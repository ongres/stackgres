/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtil.class);

  public static final BigDecimal MILLICPU_MULTIPLIER = new BigDecimal(1000);
  public static final BigDecimal LOAD_MULTIPLIER = new BigDecimal(1000);
  public static final BigDecimal KILOBYTE = new BigDecimal(1024);

  private ResourceUtil() {
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

  public static String resourceName(String name) {
    Preconditions.checkArgument(name.length() <= 253);
    return name;
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
    return "^.*-([0-9]+)$";
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
   * @param client  Kubernetes client to call the API.
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

  public static String decodeSecret(String string) {
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

  public static Optional<BigInteger> toBigInteger(String value) {
    try {
      return Optional.of(new BigInteger(value));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> toMillicpus(String cpus) {
    try {
      return Optional.of(new BigDecimal(cpus).multiply(MILLICPU_MULTIPLIER).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> toMillicpus(BigInteger cpus) {
    try {
      return Optional.of(new BigDecimal(cpus).multiply(MILLICPU_MULTIPLIER).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> kilobytesToBytes(String kilobytes) {
    try {
      return Optional.of(new BigDecimal(kilobytes).multiply(KILOBYTE).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> toMilliload(String load) {
    try {
      return Optional.of(new BigDecimal(load).multiply(LOAD_MULTIPLIER).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static String asMillicpusWithUnit(BigInteger millicpus) {
    return millicpus + "m";
  }

  public static String asBytesWithUnit(BigInteger bytes) {
    ImmutableList<String> units = ImmutableList.of(
        "Ki", "Mi", "Gi", "Ti", "Pi", "Ei");
    return units.stream().reduce(
        Tuple.tuple(new BigDecimal(bytes), ""),
        (t, unit) -> t.v1.compareTo(KILOBYTE) >= 0
        ? Tuple.tuple(t.v1.divide(KILOBYTE), unit)
            : t,
        (u, v) -> v)
        .map((value, unit) -> getDecimalFormat().format(value) + unit);
  }

  public static String asLoad(BigInteger milliload) {
    return getDecimalFormat().format(new BigDecimal(milliload).divide(LOAD_MULTIPLIER));
  }

  public static DecimalFormat getDecimalFormat() {
    return new DecimalFormat("#0.00");
  }

}
