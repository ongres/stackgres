/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

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

import javax.enterprise.inject.spi.CDI;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

public enum StackGresUtil {

  INSTANCE;

  public static final String APP_KEY = "app";
  public static final String APP_NAME = "StackGresCluster";
  public static final String CLUSTER_NAME_KEY = "cluster-name";
  public static final String CLUSTER_UID_KEY = "cluster-uid";
  public static final String CLUSTER_NAMESPACE_KEY = "cluster-namespace";
  public static final String RIGHT_VALUE = Boolean.TRUE.toString();
  public static final String WRONG_VALUE = Boolean.FALSE.toString();
  public static final String CLUSTER_KEY = "cluster";
  public static final String BACKUP_KEY = "backup";
  public static final String DISRUPTIBLE_KEY = "disruptible";
  public static final String ROLE_KEY = "role";
  public static final String PRIMARY_ROLE = "master";
  public static final String REPLICA_ROLE = "replica";
  public static final String PROMOTE_ROLE = "promote";
  public static final String DEMOTE_ROLE = "demote";
  public static final String UNINITIALIZED_ROLE = "uninitialized";
  public static final String STANDBY_LEADER_ROLE = "standby_leader";
  public static final String REST_USER_KEY = "user";
  public static final String REST_PASSWORD_KEY = "password";
  public static final String DISTRIBUTED_LOGS_APP_NAME = "StackGresDistributedLogs";
  public static final String DISTRIBUTED_LOGS_CLUSTER_NAME_KEY = "distributed-logs-name";
  public static final String DISTRIBUTED_LOGS_CLUSTER_NAMESPACE_KEY = "distributed-logs-namespace";
  public static final String DISTRIBUTED_LOGS_CLUSTER_UID_KEY = "distributed-logs-uid";
  public static final String DISTRIBUTED_LOGS_CLUSTER_KEY = "distributed-logs-cluster";
  public static final String DISTRIBUTED_LOGS_BACKUP_KEY = "distributed-logs-backup";

  public static final String KUBECTL_IMAGE = "bitnami/kubectl:1.18.3";
  public static final String BUSYBOX_IMAGE = "busybox:1.31.1";

  public static final String OPERATOR_NAME = INSTANCE.operatorName;
  public static final String OPERATOR_NAMESPACE = INSTANCE.operatorNamespace;

  public static final String PROMETHEUS_AUTOBIND = INSTANCE.prometheusAutobind;

  public static final String OPERATOR_IP = INSTANCE.operatorIp;

  public static final String AUTHENTICATION_SECRET_NAME = INSTANCE.authenticationSecretName;

  private final String operatorName;
  private final String operatorNamespace;
  private final String operatorIp;

  private final String prometheusAutobind;

  private final String authenticationSecretName;

  StackGresUtil() {
    try {
      Properties properties = new Properties();
      properties.load(StackGresUtil.class.getResourceAsStream("/application.properties"));
      Seq.seq(properties).forEach(t -> System.setProperty(
          String.class.cast(t.v1), String.class.cast(t.v2)));
      operatorName = getProperty(properties, OperatorProperty.OPERATOR_NAME);
      operatorNamespace = getProperty(properties, OperatorProperty.OPERATOR_NAMESPACE);
      prometheusAutobind = getProperty(properties, OperatorProperty.PROMETHEUS_AUTOBIND);
      operatorIp = getProperty(properties, OperatorProperty.OPERATOR_IP);
      authenticationSecretName = getProperty(properties, OperatorProperty
          .AUTHENTICATION_SECRET_NAME);
      Preconditions.checkNotNull(operatorName);
      Preconditions.checkNotNull(operatorNamespace);
      Preconditions.checkNotNull(prometheusAutobind);
      Preconditions.checkNotNull(authenticationSecretName);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Return a property value by searching first in environment variables and if not present in
   * system properties.
   */
  private static String getProperty(Properties properties, OperatorProperty configProperty) {
    return Optional.ofNullable(System.getenv(configProperty.property()))
        .orElseGet(() -> properties.getProperty(configProperty.systemProperty()));
  }

  /**
   * Return true when labels match a patroni primary pod, false otherwise.
   */
  public static boolean isPrimary(Map<String, String> labels) {
    return Objects.equals(labels.get(StackGresUtil.ROLE_KEY), StackGresUtil.PRIMARY_ROLE);
  }

  /**
   * Return true when labels match a patroni primary pod that is also disruptible, false otherwise.
   */
  public static boolean isNonDisruptiblePrimary(Map<String, String> labels) {
    return isPrimary(labels)
        && Objects.equals(labels.get(StackGresUtil.DISRUPTIBLE_KEY), StackGresUtil.WRONG_VALUE);
  }

  /**
   * Extract the index of a cluster stateful set pod.
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

  /**
   * Calculate MD5 hash of all exisitng values ordered by key.
   */
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

  /**
   * If a string URL host part starts with "www." removes it, then return the host part of the URL.
   */
  public static String getHostFromUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  /**
   * Return the port of an Web URL.
   */
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

  /**
   * This function return the namespace of the relativeId if present or the namespace.
   * <br />
   * A relative id points to a resource relative to another resource. If the resource is
   * in the same namespace of the other resource then the relativeId is the resource name.
   * If the resource is in another namespace then the relativeId will contain a '.' character
   * that separate namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  public static String getNamespaceFromRelativeId(String relativeId, String namespace) {
    final int slashIndex = relativeId.indexOf('.');
    return slashIndex >= 0
        ? relativeId.substring(0, slashIndex)
        : namespace;
  }

  /**
   * This function return the name of the relativeId.
   * <br />
   * A relative id points to a resource relative to another resource. If the resource is
   * in the same namespace of the other resource then the relativeId is the resource name.
   * If the resource is in another namespace then the relativeId will contain a '.' character
   * that separate namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  public static String getNameFromRelativeId(String relativeId) {
    final int slashIndex = relativeId.indexOf('.');
    return slashIndex >= 0
        ? relativeId.substring(slashIndex + 1)
        : relativeId;
  }

  /**
   * This function return the relative id of a name and a nanemspace
   *  relative to the relativeNamespace.
   * <br />
   * A relative id points to a resource relative to another resource. If the resource is
   * in the same namespace of the other resource then the relativeId is the resource name.
   * If the resource is in another namespace then the relativeId will contain a '.' character
   * that separate namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  public static String getRelativeId(
      String name, String namespace, String relativeNamespace) {
    if (namespace.equals(relativeNamespace)) {
      return name;
    }
    return namespace + '.' + name;
  }

  public static String toPrettyYaml(Object pojoObject) {
    try {
      try {
        return CDI.current().select(YamlMapperProvider.class).get()
            .yamlMapper().writeValueAsString(pojoObject);
      } catch (IllegalStateException ex) {
        return new YamlMapperProvider()
            .yamlMapper().writeValueAsString(pojoObject);
      }
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Failed deserializing instance of "
          + pojoObject.getClass().getName(), ex);
    }
  }

}
