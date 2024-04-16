/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource;

import static java.lang.String.format;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.net.InternetDomainName;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ResourceUtil {

  int DNS_SUBDOMAIN_NAME_MAX_LENGTH = 253;
  int DNS_LABEL_MAX_LENGTH = 63;
  int STS_DNS_LABEL_MAX_LENGTH = 52;
  int JOB_DNS_LABEL_MAX_LENGTH = 53;
  int CRON_JOB_DNS_LABEL_MAX_LENGTH = 52;

  Pattern DNS_LABEL_NAME = Pattern.compile("^[a-z]([-a-z0-9]*[a-z0-9])?$");
  Pattern VALID_VALUE =
      Pattern.compile("^(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?$");
  Pattern PREFIX_PART =
      Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");

  Logger LOGGER = LoggerFactory.getLogger(ResourceUtil.class);
  Pattern INDEX_PATTERN = Pattern.compile("^.*-([0-9]+)$");

  String KUBERNETES_NAME_KEY = "app.kubernetes.io/name";
  String KUBERNETES_INSTANCE_KEY = "app.kubernetes.io/instance";
  String KUBERNETES_VERSION_KEY = "app.kubernetes.io/version";
  String KUBERNETES_COMPONENT_KEY = "app.kubernetes.io/component";
  String KUBERNETES_PART_OF_KEY = "app.kubernetes.io/part-of";
  String KUBERNETES_MANGED_BY_KEY = "app.kubernetes.io/managed-by";
  String KUBERNETES_CREATED_BY_KEY = "app.kubernetes.io/created-by";

  /**
   * Filter metadata of resources to find if the name match in the provided list.
   *
   * @param list resources with metadata to filter
   * @param name to check for match in the list
   * @return true if the name exists in the list
   */
  static boolean exists(List<? extends HasMetadata> list, String name) {
    return list.stream()
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getName)
        .anyMatch(name::equals);
  }

  static String sanitizedResourceName(String name) {
    return name
        .toLowerCase(Locale.US)
        .replaceAll("[^a-z0-9-]+", "-")
        .replaceAll("(^-+|-+$)", "");
  }

  private static String resourceName(String name, int maxLength) {
    Preconditions.checkArgument(name.length() <= maxLength,
        format("Valid name must be %s characters or less. But was %d (%s)",
            maxLength, name.length(), name));
    Preconditions.checkArgument(DNS_LABEL_NAME.matcher(name).matches(),
        format("Name must consist of lower case alphanumeric "
            + "characters or '-', start with an alphabetic character, "
            + "and end with an alphanumeric character. But was %s", name));
    return name;
  }

  static String resourceName(String name) {
    return resourceName(name, 253);
  }

  static String nameIsValidService(String name) {
    return resourceName(name, DNS_LABEL_MAX_LENGTH);
  }

  static String nameIsValidDnsSubdomainForSts(String name) {
    return resourceName(name, STS_DNS_LABEL_MAX_LENGTH);
  }

  static String nameIsValidDnsSubdomainForJob(String name) {
    return resourceName(name, JOB_DNS_LABEL_MAX_LENGTH);
  }

  static String nameIsValidDnsSubdomainForCronJob(String name) {
    return resourceName(name, CRON_JOB_DNS_LABEL_MAX_LENGTH);
  }

  static String nameIsValidDnsSubdomain(String name) {
    return resourceName(name, DNS_SUBDOMAIN_NAME_MAX_LENGTH);
  }

  static String cutVolumeName(String name) {
    if (name.length() <= 63) {
      return name;
    }
    String cutName = name.substring(0, 63);
    if (cutName.matches("^.*[^A-Za-z0-9]$")) {
      cutName = cutName.substring(0, 62) + 'x';
    }
    return cutName;
  }

  static String containerName(String name) {
    Preconditions.checkArgument(name.length() <= 63);
    return name;
  }

  static @Nonnull String labelKey(@Nonnull String name) {
    String label = name;
    if (name.indexOf('/') != -1) {
      Preconditions.checkArgument(
          !name.startsWith("kubernetes.io/") && !name.startsWith("k8s.io/"),
          format("The kubernetes.io/ and k8s.io/ prefixes are reserved"
              + " for Kubernetes core components. But was %s", name));

      final String[] split = name.split("/");
      Preconditions.checkArgument(split.length == 2, "name part must be non-empty");

      String prefix = split[0];

      Preconditions.checkArgument(PREFIX_PART.matcher(prefix).matches(),
          format("Prefix part a lowercase RFC 1123 subdomain must consist of lower case "
              + "alphanumeric characters, '-' or '.', and must start and end "
              + "with an alphanumeric character. But was %s", prefix));

      InternetDomainName.from(prefix);

      label = split[1];
    }

    if (!label.isBlank()) {
      Preconditions.checkArgument(VALID_VALUE.matcher(label).matches(),
          "Label key not compliant with pattern %s, was %s", VALID_VALUE.pattern(), name);
    }

    Preconditions.checkArgument(label.length() <= 63,
        format("Label key must be 63 characters or less but was %d (%s)", name.length(), name));

    return name;
  }

  static @Nonnull String labelValue(@Nonnull String name) {
    if (!name.isBlank()) {
      Preconditions.checkArgument(VALID_VALUE.matcher(name).matches(),
          "Label value not compliant with pattern %s, was %s", VALID_VALUE.pattern(), name);
    }
    Preconditions.checkArgument(name.length() <= 63,
        format("Label value must be 63 characters or less but was %d (%s)", name.length(), name));
    return name;
  }

  static Pattern getIndexPattern() {
    return INDEX_PATTERN;
  }

  static int getIndexFromNameWithIndex(String name) {
    return Optional.of(name)
        .map(INDEX_PATTERN::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .map(Integer::parseInt)
        .orElse(0);
  }

  static Pattern getNameWithIndexPattern(String name) {
    return Pattern.compile(getNameWithIndexPatternString(name));
  }

  static String getNameWithIndexPatternString(@Nonnull String name) {
    return "^" + Pattern.quote(name) + "-([0-9]+)$";
  }

  static Pattern getNameWithHashPattern(String name) {
    return Pattern.compile(getNameWithHashPatternString(name));
  }

  static String getNameWithHashPatternString(@Nonnull String name) {
    return "^" + Pattern.quote(name) + "-([a-z0-9]+){10}-([a-z0-9]+){5}$";
  }

  static String getNameFromIndexedNamePatternString(@Nonnull String name) {
    return "^" + Pattern.quote(name) + "-([a-z0-9]+){10}-([a-z0-9]+){5}$";
  }

  /**
   * Get the object reference of any resource.
   */
  static ObjectReference getObjectReference(HasMetadata resource) {
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
  static OwnerReference getOwnerReference(HasMetadata resource) {
    return new OwnerReferenceBuilder()
        .withApiVersion(resource.getApiVersion())
        .withKind(resource.getKind())
        .withName(resource.getMetadata().getName())
        .withUid(resource.getMetadata().getUid())
        .build();
  }

  /**
   * Get the owner reference of any resource.
   */
  static OwnerReference getControllerOwnerReference(HasMetadata resource) {
    return new OwnerReferenceBuilder()
        .withApiVersion(resource.getApiVersion())
        .withKind(resource.getKind())
        .withName(resource.getMetadata().getName())
        .withUid(resource.getMetadata().getUid())
        .withController(true)
        .withBlockOwnerDeletion(true)
        .build();
  }

  static Map<String, String> encodeSecret(Map<String, String> data) {
    return data.entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), encodeSecret(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static String encodeSecret(String string) {
    return Base64.getEncoder().encodeToString(string.getBytes(StandardCharsets.UTF_8));
  }

  static Map<String, String> decodeSecret(Map<String, String> data) {
    return data.entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), decodeSecret(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static String decodeSecret(String string) {
    return new String(Base64.getDecoder().decode(string.getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8);
  }

  static boolean isServiceAccountUsername(String username) {
    return username.startsWith("system:serviceaccount:") && username.split(":").length == 4;
  }

  static String getServiceAccountFromUsername(String username) {
    return username.split(":")[3];
  }

}
