/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.StackGresContext.LOCK_POD_KEY;
import static io.stackgres.common.StackGresContext.LOCK_SERVICE_ACCOUNT_KEY;
import static io.stackgres.common.StackGresContext.LOCK_TIMESTAMP_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.enterprise.inject.spi.CDI;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface StackGresUtil {

  String DATA_SUFFIX = "-data";
  String BACKUP_SUFFIX = "-backup";

  static String statefulSetDataPersistentVolumeName(ClusterContext cluster) {
    return ResourceUtil
        .nameIsValidService(cluster.getCluster().getMetadata().getName() + DATA_SUFFIX);
  }

  static String statefulSetDataPersistentVolumeName(CustomResource<?, ?> cluster) {
    return ResourceUtil.nameIsValidService(cluster.getMetadata().getName() + DATA_SUFFIX);
  }

  static String statefulSetBackupPersistentVolumeName(StackGresCluster cluster) {
    return ResourceUtil.nameIsValidService(cluster.getMetadata().getName() + BACKUP_SUFFIX);
  }

  /**
   * This function return the namespace of the relativeId if present or the namespace.
   *
   * <p>A relative id points to a resource relative to another resource. If the resource is in the
   * same namespace of the other resource then the relativeId is the resource name. If the resource
   * is in another namespace then the relativeId will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).
   */
  static String getNamespaceFromRelativeId(String relativeId, String namespace) {
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
  static String getNameFromRelativeId(String relativeId) {
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
  static String getRelativeId(
      String name, String namespace, String relativeNamespace) {
    if (namespace.equals(relativeNamespace)) {
      return name;
    }
    return namespace + '.' + name;
  }

  /**
   * Return true when labels match a non-disruptible label, false otherwise.
   */
  static boolean isNonDisruptible(Map<String, String> labels) {
    return Objects.equals(labels.get(StackGresContext.DISRUPTIBLE_KEY),
            StackGresContext.WRONG_VALUE);
  }

  /**
   * Extract the index of a cluster stateful set pod.
   */
  static Integer extractPodIndex(StackGresCluster cluster, ObjectMeta podMetadata) {
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
  static Map<String, String> addMd5Sum(Map<String, String> data) {
    MessageDigest messageDigest = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    messageDigest.update(data.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .collect(Collectors.joining())
        .getBytes(StandardCharsets.UTF_8));
    return ImmutableMap.<String, String>builder()
        .putAll(data)
        .put("MD5SUM", BaseEncoding.base16().encode(messageDigest.digest()))
        .build();
  }

  /**
   * Calculate MD5 hash of all files ordered by path.
   */
  static String getMd5Sum(Path... paths) {
    MessageDigest messageDigest = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    Seq.of(paths)
        .sorted()
        .map(Unchecked.function(Files::readAllBytes))
        .forEach(messageDigest::update);
    return BaseEncoding.base16().encode(messageDigest.digest());
  }

  /**
   * If a string URL host part starts with "www." removes it, then return the host part of the URL.
   */
  static String getHostFromUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String domain = uri.getHost();
    return domain.startsWith("www.") ? domain.substring(4) : domain;
  }

  /**
   * Return the port of an Web URL.
   */
  static int getPortFromUrl(String url) throws MalformedURLException {
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
   * @throws UncheckedIOException if cannot load the properties file
   */
  static @NotNull Properties loadProperties(@NotNull String path) {
    try (InputStream is = StackGresUtil.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Cannot load the properties file: " + path);
      }
      Properties props = new Properties();
      props.load(is);
      return props;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @NotNull
  static String toPrettyYaml(Object pojoObject) {
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
  static String getServiceDnsName(@NotNull Service service) {
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
      serviceDns = metadata.getName() + '.' + metadata.getNamespace();
    }
    return serviceDns;
  }

  static ImmutableList<Tuple2<String, Optional<String>>> getDefaultClusterExtensions(
      StackGresCluster cluster) {
    if (getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      return ImmutableList.of();
    }
    if (StackGresComponent.compareBuildVersions("6.6",
        StackGresComponent.PATRONI.findBuildVersion(
            StackGresComponent.LATEST, ImmutableMap.of(
                getPostgresFlavorComponent(cluster),
                cluster.getSpec().getPostgres().getVersion()))) <= 0) {
      return ImmutableList.of();
    }
    return Seq.of(
        Tuple.tuple("plpgsql"),
        Tuple.tuple("pg_stat_statements"),
        Tuple.tuple("dblink"),
        Tuple.tuple("plpython3u"))
        .map(t -> t.concat(Optional.<String>empty()))
        .collect(ImmutableList.toImmutableList());
  }

  static ImmutableList<Tuple2<String, Optional<String>>> getDefaultDistributedLogsExtensions(
      StackGresCluster cluster) {
    return Seq.seq(getDefaultClusterExtensions(cluster))
        .append(Tuple.tuple("timescaledb", Optional.of("1.7.4")))
        .collect(ImmutableList.toImmutableList());
  }

  static boolean isLocked(HasMetadata resource, int lockTimeoutMillis) {
    long currentTimeSeconds = System.currentTimeMillis() / 1000;
    long timedOutLock = currentTimeSeconds - lockTimeoutMillis;
    return Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .filter(annotation -> annotation.containsKey(LOCK_POD_KEY)
            && annotation.containsKey(LOCK_TIMESTAMP_KEY))
        .map(annotations -> Long.parseLong(annotations.get(LOCK_TIMESTAMP_KEY)))
        .map(lockTimestamp -> lockTimestamp > timedOutLock)
        .orElse(false);
  }

  static boolean isLockedByMe(HasMetadata resource, String lockPodName) {
    return Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .filter(annotation -> annotation.containsKey(LOCK_POD_KEY)
            && annotation.containsKey(LOCK_TIMESTAMP_KEY))
        .map(annotation -> annotation.get(LOCK_POD_KEY).equals(lockPodName))
        .orElse(false);
  }

  static void setLock(HasMetadata resource, String lockServiceAccount, String lockPodName,
      long lockTimestamp) {
    final Map<String, String> annotations = resource.getMetadata().getAnnotations();

    annotations.put(LOCK_SERVICE_ACCOUNT_KEY, lockServiceAccount);
    annotations.put(LOCK_POD_KEY, lockPodName);
    annotations.put(LOCK_TIMESTAMP_KEY, Long.toString(lockTimestamp));
  }

  static void resetLock(HasMetadata resource) {
    final Map<String, String> annotations = resource.getMetadata().getAnnotations();

    annotations.remove(LOCK_SERVICE_ACCOUNT_KEY);
    annotations.remove(LOCK_POD_KEY);
    annotations.remove(LOCK_TIMESTAMP_KEY);
  }

  static String getLockServiceAccount(HasMetadata resource) {
    return Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(LOCK_SERVICE_ACCOUNT_KEY))
        .orElseThrow(() -> new IllegalArgumentException(
            "Resource not locked or locked and annotation "
                + LOCK_SERVICE_ACCOUNT_KEY + " not set"));
  }

  static String getPatroniImageName(StackGresCluster cluster) {
    return getPatroniImageName(cluster, cluster.getSpec().getPostgres().getVersion());
  }

  static String getPatroniImageName(StackGresCluster cluster, String postgresVersion) {
    StackGresComponent postgresComponentFlavor = getPostgresFlavorComponent(cluster);
    return StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(postgresComponentFlavor,
            postgresVersion));
  }

  static StackGresComponent getPostgresFlavorComponent(StackGresCluster cluster) {
    return getPostgresFlavorComponent(cluster.getSpec().getPostgres().getFlavor());
  }

  static StackGresComponent getPostgresFlavorComponent(@Nullable String flavor) {
    StackGresComponent postgresComponentFlavor;
    if (Optional.ofNullable(flavor)
        .map(StackGresPostgresFlavor.VANILLA.toString()::equals)
        .orElse(true)) {
      postgresComponentFlavor = StackGresComponent.POSTGRESQL;
    } else if (Optional.ofNullable(flavor)
        .map(StackGresPostgresFlavor.BABELFISH.toString()::equals)
        .orElse(false)) {
      postgresComponentFlavor = StackGresComponent.BABELFISH;
    } else {
      throw new IllegalArgumentException("Unknown flavor " + flavor);
    }
    return postgresComponentFlavor;
  }

  /**
   * This is a best-effort to parse the /etc/resolv.conf file and get the search path of K8s.
   */
  static String domainSearchPath() {
    ResolvConfResolverConfig resolver = new ResolvConfResolverConfig();
    List<String> searchPath = resolver.getSearchPath("/etc/resolv.conf");
    for (var sp : searchPath) {
      if (sp.startsWith("svc.")) {
        return "." + sp;
      }
    }
    // fallback default value
    return ".svc.cluster.local";
  }

}
