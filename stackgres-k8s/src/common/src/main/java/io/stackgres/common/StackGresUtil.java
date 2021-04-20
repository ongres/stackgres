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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.CDI;
import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

public class StackGresUtil {

  public static final String DATA_SUFFIX = "-data";
  public static final String BACKUP_SUFFIX = "-backup";

  private static final String DNS_SERVICE = "svc.cluster.local";

  public static String statefulSetDataPersistentVolumeName(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + DATA_SUFFIX);
  }

  public static String statefulSetBackupPersistentVolumeName(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + BACKUP_SUFFIX);
  }

  /**
   * This function return the namespace of the relativeId if present or the namespace.
   *
   * <p>A relative id points to a resource relative to another resource. If the resource is in the
   * same namespace of the other resource then the relativeId is the resource name. If the resource
   * is in another namespace then the relativeId will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  public static String getNamespaceFromRelativeId(String relativeId, String namespace) {
    final int slashIndex = relativeId.indexOf('.');
    return slashIndex >= 0
        ? relativeId.substring(0, slashIndex)
        : namespace;
  }

  /**
   * This function return the name of the relativeId.
   *
   * <p>A relative id points to a resource relative to another resource. If the resource is in the
   * same namespace of the other resource then the relativeId is the resource name. If the resource
   * is in another namespace then the relativeId will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  public static String getNameFromRelativeId(String relativeId) {
    final int slashIndex = relativeId.indexOf('.');
    return slashIndex >= 0
        ? relativeId.substring(slashIndex + 1)
        : relativeId;
  }

  /**
   * This function return the relative id of a name and a nanemspace relative to the
   * relativeNamespace.
   *
   * <p>A relative id points to a resource relative to another resource. If the resource is in the
   * same namespace of the other resource then the relativeId is the resource name. If the resource
   * is in another namespace then the relativeId will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  public static String getRelativeId(
      String name, String namespace, String relativeNamespace) {
    if (namespace.equals(relativeNamespace)) {
      return name;
    }
    return namespace + '.' + name;
  }

  /**
   * Return true when labels match a patroni primary pod, false otherwise.
   */
  public static boolean isPrimary(Map<String, String> labels) {
    return Objects.equals(labels.get(StackGresContext.ROLE_KEY), StackGresContext.PRIMARY_ROLE);
  }

  /**
   * Return true when labels match a patroni primary pod that is also disruptible, false otherwise.
   */
  public static boolean isNonDisruptiblePrimary(Map<String, String> labels) {
    return isPrimary(labels)
        && Objects.equals(labels.get(StackGresContext.DISRUPTIBLE_KEY),
            StackGresContext.WRONG_VALUE);
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
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(e -> e.getValue())
        .collect(Collectors.joining())
        .getBytes(StandardCharsets.UTF_8));
    return ImmutableMap.<String, String>builder()
        .putAll(data)
        .put("MD5SUM", DatatypeConverter.printHexBinary(
            messageDigest.digest()).toUpperCase(Locale.US))
        .build();
  }

  /**
   * Calculate MD5 hash of all files ordered by path.
   */
  public static String getMd5Sum(Path... paths) {
    MessageDigest messageDigest = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    Seq.of(paths)
        .sorted()
        .map(Unchecked.function(Files::readAllBytes))
        .forEach(messageDigest::update);
    return DatatypeConverter.printHexBinary(
        messageDigest.digest()).toUpperCase(Locale.US);
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
   *
   * @param path the path of the properties file to load
   * @return the loaded file
   * @throws IOException if cannot load the properties file
   */
  public static Properties loadProperties(@NotNull String path) throws IOException {
    try (InputStream is = ClassLoader.getSystemResourceAsStream(path)) {
      if (is != null) {
        Properties props = new Properties();
        props.load(is);
        return props;
      } else {
        throw new IOException("cannot load the properties file");
      }
    }
  }

  @NotNull
  public static String toPrettyYaml(Object pojoObject) {
    try {
      try {
        return CDI.current().select(YamlMapperProvider.class).get()
            .yamlMapper().writeValueAsString(pojoObject);
      } catch (Exception ex) {
        return new YamlMapperProvider()
            .yamlMapper().writeValueAsString(pojoObject);
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed deserializing instance of "
          + pojoObject.getClass().getName(), ex);
    }
  }

  /**
   * Extract the hostname from a LoadBalancer service or get the Internal FQDN of the service.
   *
   * @param service name.
   * @return String fqdn of the provided service.
   * @throws IllegalStateException if the service is invalid.
   */
  @NotNull
  public static String getServiceDnsName(@NotNull Service service) {
    String serviceDns = null;
    ServiceStatus status = service.getStatus();
    if (status != null && "LoadBalancer".equals(service.getSpec().getType())) {
      List<LoadBalancerIngress> ingress = status.getLoadBalancer().getIngress();
      if (ingress != null && !ingress.isEmpty()) {
        LoadBalancerIngress loadBalancerIngress = ingress.get(0);
        serviceDns = loadBalancerIngress.getHostname() != null
            ? loadBalancerIngress.getHostname()
            : loadBalancerIngress.getIp();
      }
    }
    if (serviceDns == null) {
      ObjectMeta metadata = service.getMetadata();
      if (metadata.getName() == null || metadata.getNamespace() == null) {
        throw new IllegalStateException(
            "Invalid service definition, name and namespace are required.");
      }
      serviceDns = metadata.getName() + '.' + metadata.getNamespace()
          + '.' + DNS_SERVICE;
    }
    return serviceDns;
  }

}
