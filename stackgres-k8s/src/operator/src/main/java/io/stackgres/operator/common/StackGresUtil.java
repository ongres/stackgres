/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Unchecked;

public enum StackGresUtil {

  INSTANCE;

  public static final String APP_KEY = "app";
  public static final String APP_NAME = "StackGres";
  public static final String CLUSTER_NAME_KEY = "cluster-name";
  public static final String CLUSTER_UID_KEY = "cluster-uid";
  public static final String CLUSTER_NAMESPACE_KEY = "cluster-namespace";
  public static final String CLUSTER_KEY = "cluster";
  public static final String BACKUP_KEY = "backup";
  public static final String DISRUPTIBLE_KEY = "disruptible";
  public static final String ROLE_KEY = "role";
  public static final String PRIMARY_ROLE = "master";
  public static final String REPLICA_ROLE = "replica";

  public static final String OPERATOR_NAME = INSTANCE.operatorName;
  public static final String OPERATOR_NAMESPACE = INSTANCE.operatorNamespace;
  public static final String OPERATOR_VERSION = INSTANCE.operatorVersion;

  public static final String CRD_GROUP = INSTANCE.group;
  public static final String CRD_VERSION = INSTANCE.version;

  public static final String CONTAINER_BUILD = INSTANCE.containerBuild;

  public static final String PROMETHEUS_AUTOBIND = INSTANCE.prometheusAutobind;

  public static final String OPERATOR_IP = INSTANCE.operatorIp;

  public static final String DOCUMENTATION_URI = INSTANCE.documentationUri;

  public static final String DOCUMENTATION_ERRORS_PATH = INSTANCE.documentationErrorsPath;

  public static final String AUTHENTICATION_SECRET_NAME = INSTANCE.authenticationSecretName;

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorVersion;
  private final String operatorIp;

  private final String group;
  private final String version;

  private final String containerBuild;

  private final String prometheusAutobind;

  private final String documentationUri;
  private final String documentationErrorsPath;

  private final String authenticationSecretName;

  StackGresUtil() {
    try {
      Properties properties = new Properties();
      properties.load(StackGresUtil.class.getResourceAsStream("/application.properties"));
      operatorName = getProperty(properties, ConfigProperty.OPERATOR_NAME);
      operatorNamespace = getProperty(properties, ConfigProperty.OPERATOR_NAMESPACE);
      operatorVersion = getProperty(properties, ConfigProperty.OPERATOR_VERSION);
      group = getProperty(properties, ConfigProperty.CRD_GROUP);
      version = getProperty(properties, ConfigProperty.CRD_VERSION);
      containerBuild = getProperty(properties, ConfigProperty.CONTAINER_BUILD);
      prometheusAutobind = getProperty(properties, ConfigProperty.PROMETHEUS_AUTOBIND);
      operatorIp = getProperty(properties, ConfigProperty.OPERATOR_IP);
      documentationUri = getProperty(properties, ConfigProperty.DOCUMENTATION_URI);
      documentationErrorsPath = getProperty(properties, ConfigProperty.DOCUMENTATION_ERRORS_PATH);
      authenticationSecretName = getProperty(properties, ConfigProperty.AUTHENTICATION_SECRET_NAME);
      Preconditions.checkNotNull(operatorName);
      Preconditions.checkNotNull(operatorNamespace);
      Preconditions.checkNotNull(operatorVersion);
      Preconditions.checkNotNull(group);
      Preconditions.checkNotNull(version);
      Preconditions.checkNotNull(containerBuild);
      Preconditions.checkNotNull(prometheusAutobind);
      Preconditions.checkNotNull(documentationUri);
      Preconditions.checkNotNull(documentationErrorsPath);
      Preconditions.checkNotNull(authenticationSecretName);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String getProperty(Properties properties, ConfigProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> properties.getProperty(configProperty.systemProperty()));
  }

  public static String clusterUid(StackGresCluster cluster) {
    return cluster.getMetadata().getUid();
  }

  public static String clusterName(StackGresCluster cluster) {
    return cluster.getMetadata().getName();
  }

  public static String clusterScopeKey() {
    return ResourceUtil.labelKey(CLUSTER_NAME_KEY);
  }

  public static String clusterScope(StackGresCluster cluster) {
    return ResourceUtil.labelValue(clusterName(cluster));
  }

  /**
   * ImmutableMap of cluster labels used as selectors in K8s resources.
   */
  public static ImmutableMap<String, String> defaultLabels() {
    return ImmutableMap.of(APP_KEY, APP_NAME);
  }

  /**
   * ImmutableMap of cluster labels used as selectors in K8s resources.
   */
  public static ImmutableMap<String, String> clusterLabels(StackGresCluster cluster) {
    return ImmutableMap.of(APP_KEY, APP_NAME,
        CLUSTER_UID_KEY, ResourceUtil.labelValue(clusterUid(cluster)),
        CLUSTER_NAME_KEY, ResourceUtil.labelValue(clusterName(cluster)));
  }

  /**
   * ImmutableMap of cluster labels used as selectors in K8s resources
   * outside of the namespace of the cluster.
   */
  public static ImmutableMap<String, String> clusterCrossNamespaceLabels(
      StackGresCluster cluster) {
    return ImmutableMap.of(APP_KEY, APP_NAME,
        CLUSTER_NAMESPACE_KEY, ResourceUtil.labelValue(cluster.getMetadata().getNamespace()),
        CLUSTER_UID_KEY, ResourceUtil.labelValue(clusterUid(cluster)),
        CLUSTER_NAME_KEY, ResourceUtil.labelValue(clusterName(cluster)));
  }

  /**
   * ImmutableMap of default labels used as selectors in K8s pods
   * that are part of the cluster.
   */
  public static ImmutableMap<String, String> statefulSetPodLabels(StackGresCluster cluster) {
    return ImmutableMap.of(APP_KEY, APP_NAME,
        CLUSTER_UID_KEY, ResourceUtil.labelValue(clusterUid(cluster)),
        CLUSTER_NAME_KEY, ResourceUtil.labelValue(clusterName(cluster)),
        CLUSTER_KEY, Boolean.TRUE.toString(), DISRUPTIBLE_KEY, Boolean.TRUE.toString());
  }

  /**
   * ImmutableMap of default labels used as selectors in K8s pods
   * that are part of the cluster.
   */
  public static ImmutableMap<String, String> patroniClusterLabels(StackGresCluster cluster) {
    return ImmutableMap.of(APP_KEY, APP_NAME,
        CLUSTER_UID_KEY, ResourceUtil.labelValue(clusterUid(cluster)),
        CLUSTER_NAME_KEY, ResourceUtil.labelValue(clusterName(cluster)),
        CLUSTER_KEY, Boolean.TRUE.toString());
  }

  /**
   * ImmutableMap of default labels used as selectors in K8s pods
   * that are part of any cluster.
   */
  public static ImmutableMap<String, String> patroniClusterLabels() {
    return ImmutableMap.of(APP_KEY, APP_NAME,
        CLUSTER_KEY, Boolean.TRUE.toString());
  }

  /**
   * ImmutableMap of default labels used as selectors in K8s pods
   * that work on backups.
   */
  public static ImmutableMap<String, String> backupPodLabels(StackGresCluster cluster) {
    return ImmutableMap.of(APP_KEY, APP_NAME,
        CLUSTER_UID_KEY, ResourceUtil.labelValue(clusterUid(cluster)),
        CLUSTER_NAME_KEY, ResourceUtil.labelValue(clusterName(cluster)),
        BACKUP_KEY, Boolean.TRUE.toString());
  }

  public static boolean isPrimary(Map<String, String> labels) {
    return Objects.equals(labels.get(StackGresUtil.ROLE_KEY), StackGresUtil.PRIMARY_ROLE);
  }

  public static boolean isNonDisruptiblePrimary(Map<String, String> labels) {
    return isPrimary(labels)
        && Objects.equals(labels.get(StackGresUtil.DISRUPTIBLE_KEY), Boolean.FALSE.toString());
  }

  /**
   * Extract the index of a StatefulSet's pod.
   */
  public static Integer extractPodIndex(StackGresCluster cluster, ObjectMeta podMetadata) {
    Matcher matcher = Pattern.compile(ResourceUtil.getNameWithIndexPattern(
        cluster.getMetadata().getName())).matcher(podMetadata.getName());
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    throw new IllegalStateException("Can not extract index from pod "
        + podMetadata.getNamespace() + "." + podMetadata.getName() + " for cluster "
        + cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName());
  }

  public static Map<String, String> addMd5Sum(Map<String, String> data) {
    MessageDigest messageDigest = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    messageDigest.update(data.entrySet().stream()
        .sorted((left, right) -> left.getKey().compareTo(right.getKey()))
        .map(e -> e.getValue())
        .collect(Collectors.joining())
        .getBytes());
    return ImmutableMap.<String, String>builder()
        .putAll(data)
        .put("MD5SUM", DatatypeConverter.printHexBinary(
            messageDigest.digest()).toUpperCase(Locale.US))
        .build();
  }

  public static String getHostFromUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  public static int getPortFromUrl(String url) throws MalformedURLException {
    URL parsedUrl = new URL(url);
    int port = parsedUrl.getPort();
    if (port == -1) {
      if (parsedUrl.getProtocol().equals("https")) {
        return 443;
      } else {
        return 80;
      }
    } else {
      return port;
    }
  }

  /**
   * Loads a properties file from the classpath.
   * @param path the path of the properties file to load
   * @return the loaded file
   * @throws IOException if cannot load the properties file
   */
  public static Properties loadProperties(String path) throws IOException {
    try (InputStream is = ClassLoader
        .getSystemResourceAsStream(path)) {
      Properties props = new Properties();
      props.load(is);
      return props;
    }
  }
}
