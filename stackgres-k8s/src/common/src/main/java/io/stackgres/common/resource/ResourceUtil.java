/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.net.InternetDomainName;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtil.class);

  private static final int DNS_SUBDOMAIN_NAME_MAX_LENGTH = 253;
  private static final int DNS_LABEL_MAX_LENGTH = 63;
  private static final int STS_DNS_LABEL_MAX_LENGTH = 52;
  private static final int JOB_DNS_LABEL_MAX_LENGTH = 53;
  private static final int CRON_JOB_DNS_LABEL_MAX_LENGTH = 52;

  public static final BigDecimal MILLICPU_MULTIPLIER = new BigDecimal(1000);
  public static final BigDecimal LOAD_MULTIPLIER = new BigDecimal(1000);
  public static final BigDecimal KILOBYTE = Quantity.getAmountInBytes(new Quantity("1Ki"));
  public static final BigDecimal MEBIBYTES = Quantity.getAmountInBytes(new Quantity("1Mi"));
  public static final BigDecimal GIBIBYTES = Quantity.getAmountInBytes(new Quantity("1Gi"));

  public static final Pattern DNS_LABEL_NAME = Pattern.compile("^[a-z]([-a-z0-9]*[a-z0-9])?$");
  private static final Pattern VALID_VALUE =
      Pattern.compile("^(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?$");
  private static final Pattern PREFIX_PART =
      Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");

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

  private static String resourceName(String name, int maxLength) {
    Preconditions.checkArgument(name.length() <= maxLength,
        format("Valid name must be %s characters or less", maxLength));
    Preconditions.checkArgument(DNS_LABEL_NAME.matcher(name).matches(),
        "Name must consist of lower case alphanumeric "
            + "characters or '-', start with an alphabetic character, "
            + "and end with an alphanumeric character");
    return name;
  }

  public static String nameIsValidService(String name) {
    return resourceName(name, DNS_LABEL_MAX_LENGTH);
  }

  public static String nameIsValidDnsSubdomainForSts(String name) {
    return resourceName(name, STS_DNS_LABEL_MAX_LENGTH);
  }

  public static String nameIsValidDnsSubdomainForJob(String name) {
    return resourceName(name, JOB_DNS_LABEL_MAX_LENGTH);
  }

  public static String nameIsValidDnsSubdomainForCronJob(String name) {
    return resourceName(name, CRON_JOB_DNS_LABEL_MAX_LENGTH);
  }

  public static String nameIsValidDnsSubdomain(String name) {
    return resourceName(name, DNS_SUBDOMAIN_NAME_MAX_LENGTH);
  }

  public static String containerName(String name) {
    Preconditions.checkArgument(name.length() <= 63);
    return name;
  }

  public static @NotNull String labelKey(@NotNull String name) {
    String label = name;
    if (name.indexOf('/') != -1) {
      Preconditions.checkArgument(
          !name.startsWith("kubernetes.io/") && !name.startsWith("k8s.io/"),
          "The kubernetes.io/ and k8s.io/ prefixes are reserved for Kubernetes core components");

      final String[] split = name.split("/");
      Preconditions.checkArgument(split.length == 2, "name part must be non-empty");

      String prefix = split[0];

      Preconditions.checkArgument(PREFIX_PART.matcher(prefix).matches(),
          "prefix part a lowercase RFC 1123 subdomain must consist of lower case "
              + "alphanumeric characters, '-' or '.', and must start and end "
              + "with an alphanumeric character");

      InternetDomainName.from(prefix);

      label = split[1];
    }

    if (!label.isBlank()) {
      Preconditions.checkArgument(VALID_VALUE.matcher(label).matches(),
          "Not a valid label value");
    }

    Preconditions.checkArgument(label.length() <= 63,
        "Valid label must be 63 characters or less");

    return name;
  }

  public static @NotNull String labelValue(@NotNull String name) {
    if (!name.isBlank()) {
      Preconditions.checkArgument(VALID_VALUE.matcher(name).matches(),
          "Not a valid label value");
    }
    Preconditions.checkArgument(name.length() <= 63,
        "Valid name must be 63 characters or less");
    return name;
  }

  public static String getNameWithIndexPattern(@NotNull String name) {
    return "^" + Pattern.quote(name) + "-([0-9]+)$";
  }

  public static String getNameWithHashPattern(@NotNull String name) {
    return "^" + Pattern.quote(name) + "-([a-z0-9]+){10}-([a-z0-9]+){5}$";
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
    return new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
  }

}
