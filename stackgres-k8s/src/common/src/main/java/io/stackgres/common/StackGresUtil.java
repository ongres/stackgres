/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.StackGresContext.LOCK_POD_KEY;
import static io.stackgres.common.StackGresContext.LOCK_SERVICE_ACCOUNT_KEY;
import static io.stackgres.common.StackGresContext.LOCK_TIMEOUT_KEY;

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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceStatus;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.stackgres.common.component.Component;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgconfig.StackGresConfigAdminui;
import io.stackgres.common.crd.sgconfig.StackGresConfigImage;
import io.stackgres.common.crd.sgconfig.StackGresConfigJobs;
import io.stackgres.common.crd.sgconfig.StackGresConfigRestapi;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface StackGresUtil {

  String DISTRIBUTEDLOGS_POSTGRES_VERSION = "12";
  String MD5SUM_KEY = "MD5SUM";
  String MD5SUM_2_KEY = MD5SUM_KEY + "_2";
  String DATA_SUFFIX = "-data";
  String BACKUP_SUFFIX = "-backup";
  Pattern EMPTY_LINE_PATTERN = Pattern.compile(
      "^\\s*(:?#.*)?$");
  Pattern PARAMETER_PATTERN = Pattern.compile(
      "^\\s*(?<parameter>[^\\s=]+)"
          + "\\s*[=\\s]\\s*"
          + "(?:'(?<quoted>.*)'|(?<unquoted>(?:|[^'\\s#][^\\s#]*)))(?:\\s*#.*)?\\s*$");
  Pattern IMAGE_NAME_WITH_REGISTRY_PATTERN =
      Pattern.compile("^[^/]+\\.[^/]+/.*$");

  static String statefulSetDataPersistentVolumeClaimName(ClusterContext cluster) {
    return ResourceUtil.resourceName(cluster.getCluster().getMetadata().getName() + DATA_SUFFIX);
  }

  static String statefulSetDataPersistentVolumeClaimName(CustomResource<?, ?> cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + DATA_SUFFIX);
  }

  static String cronJobBackupName(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + BACKUP_SUFFIX);
  }

  static String cronJobShardedBackupName(StackGresShardedCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + BACKUP_SUFFIX);
  }

  static String statefulSetPodDataPersistentVolumeClaimName(CustomResource<?, ?> cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName() + DATA_SUFFIX
        + "-" + cluster.getMetadata().getName());
  }

  /**
   * This function return the namespace of the relativeId if present or the namespace.
   *
   * <p>A relative id points to a resource relative to another resource. If the resource is in the
   * same namespace of the other resource then the relative id is the resource name. If the resource
   * is in another namespace then the relative id will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).</p>
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
   * same namespace of the other resource then the relative id is the resource name. If the resource
   * is in another namespace then the relative id will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).</p>
   */
  static String getNameFromRelativeId(String relativeId) {
    final int slashIndex = relativeId.indexOf('.');
    return slashIndex >= 0
        ? relativeId.substring(slashIndex + 1)
        : relativeId;
  }

  /**
   * This function return true only if the relative id is in the same namespace as the relative
   *  resource, false otherwise.
   *
   * <p>A relative id points to a resource relative to another resource. If the resource is in the
   * same namespace of the other resource then the relative id is the resource name. If the resource
   * is in another namespace then the relative id will contain a '.' character that separate
   * namespace and name (`&lt;namespace&gt;.&lt;name&gt;`).</p>
   */
  static boolean isRelativeIdNotInSameNamespace(String relativeId) {
    return relativeId.indexOf('.') >= 0;
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
   * Calculate MD5 hash of all exisitng values ordered by key.
   */
  static Map<String, String> addMd5Sum(Map<String, String> data) {
    MessageDigest messageDigest2 = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    messageDigest2.update(data.entrySet().stream()
        .filter(entry -> !entry.getKey().startsWith(MD5SUM_KEY))
        .sorted(Map.Entry.comparingByKey())
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining())
        .getBytes(StandardCharsets.UTF_8));
    data = ImmutableMap.<String, String>builder()
        .putAll(data.entrySet().stream()
            .filter(entry -> !entry.getKey().startsWith(MD5SUM_KEY))
            .toList())
        .put(MD5SUM_2_KEY, HexFormat.of().withUpperCase().formatHex(messageDigest2.digest()))
        .build();
    MessageDigest messageDigest = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    messageDigest.update(data.entrySet().stream()
        .filter(entry -> !entry.getKey().equals(MD5SUM_KEY))
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .collect(Collectors.joining())
        .getBytes(StandardCharsets.UTF_8));
    return ImmutableMap.<String, String>builder()
        .putAll(data.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(MD5SUM_KEY))
            .toList())
        .put(MD5SUM_KEY, HexFormat.of().withUpperCase().formatHex(messageDigest.digest()))
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
        .flatMap(path -> Stream.concat(
            Stream.of(path.toString()
                .getBytes(StandardCharsets.UTF_8)),
            Optional.of(path)
            .map(Unchecked.function(Files::readAllBytes))
            .stream()))
        .forEach(messageDigest::update);
    return HexFormat.of().withUpperCase().formatHex(messageDigest.digest());
  }

  /**
   * Calculate MD5 hash of all strings.
   */
  static String getMd5Sum(String... strings) {
    MessageDigest messageDigest = Unchecked
        .supplier(() -> MessageDigest.getInstance("MD5")).get();
    Seq.of(strings)
        .filter(Objects::nonNull)
        .map(string -> string.getBytes(StandardCharsets.UTF_8))
        .forEach(messageDigest::update);
    return HexFormat.of().withUpperCase().formatHex(messageDigest.digest());
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
    URL parsedUrl = URI.create(url).toURL();
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
   * @param  path                 the path of the properties file to load
   * @return                      the loaded file
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
    YamlMapperProvider yamlProvider = null;
    ArcContainer container = Arc.container();
    if (container != null) {
      yamlProvider = container.instance(YamlMapperProvider.class).get();
    }
    try {
      return yamlProvider != null
          ? yamlProvider.get().writeValueAsString(pojoObject)
          : new YamlMapperProvider().get().writeValueAsString(pojoObject);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Failed serializing instance of "
          + pojoObject.getClass().getName(), ex);
    }
  }

  /**
   * Extract the hostname from a LoadBalancer service or get the Internal FQDN of the service.
   *
   * @param  service               name.
   * @return                       String fqdn of the provided service.
   * @throws IllegalStateException if the service is invalid.
   */
  @NotNull
  static String getServiceDnsName(@NotNull Service service) {
    String serviceDns = null;
    ServiceStatus status = service.getStatus();
    if (status != null && "LoadBalancer".equals(service.getSpec().getType())) {
      List<LoadBalancerIngress> ingress = status.getLoadBalancer().getIngress();
      if (ingress != null && !ingress.isEmpty()) {
        LoadBalancerIngress loadBalancerIngress = ingress.getFirst();
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

  static List<ExtensionTuple> getDefaultClusterExtensions(
      StackGresCluster cluster) {

    String pgVersion = cluster.getSpec().getPostgres().getVersion();
    StackGresComponent flavor = getPostgresFlavorComponent(cluster);
    StackGresVersion version = StackGresVersion.getStackGresVersion(cluster);
    return getDefaultClusterExtensions(pgVersion, flavor, version);
  }

  static List<ExtensionTuple> getDefaultClusterExtensions(
      StackGresVersion sgVersion, String pgVersion, String flavor) {
    if (Component.compareBuildVersions("6.6",
        StackGresComponent.PATRONI.getOrThrow(sgVersion)
            .getBuildVersion(StackGresComponent.LATEST, Map.of(
                getPostgresFlavorComponent(flavor).getOrThrow(sgVersion),
                pgVersion))) <= 0) {
      return List.of();
    }

    return List.of(new ExtensionTuple("plpgsql"),
        new ExtensionTuple("pg_stat_statements"),
        new ExtensionTuple("dblink"),
        new ExtensionTuple("plpython3u"));
  }

  static List<ExtensionTuple> getDefaultClusterExtensions(
      String pgVersion, StackGresComponent flavor, StackGresVersion sgVersion) {
    if (flavor == StackGresComponent.BABELFISH) {
      return List.of();
    }
    if (Component.compareBuildVersions("6.6",
        StackGresComponent.PATRONI.getOrThrow(sgVersion)
            .getBuildVersion(StackGresComponent.LATEST, Map.of(
                flavor.getOrThrow(sgVersion),
                pgVersion))) <= 0) {
      return List.of();
    }
    return List.of(new ExtensionTuple("plpgsql"),
        new ExtensionTuple("pg_stat_statements"),
        new ExtensionTuple("dblink"),
        new ExtensionTuple("plpython3u"));
  }

  static List<ExtensionTuple> getDefaultShardedClusterExtensions(
      StackGresShardedCluster cluster) {
    if (StackGresShardingType.CITUS.equals(
        StackGresShardingType.fromString(cluster.getSpec().getType()))) {
      return getDefaultCitusShardedClusterExtensions(cluster);
    }
    if (StackGresShardingType.DDP.equals(
        StackGresShardingType.fromString(cluster.getSpec().getType()))) {
      return getDefaultDdpShardedClusterExtensions(cluster);
    }
    if (StackGresShardingType.SHARDING_SPHERE.equals(
        StackGresShardingType.fromString(cluster.getSpec().getType()))) {
      return getDefaultShardingSphereShardedClusterExtensions(cluster);
    }
    return List.of();
  }

  static List<ExtensionTuple> getDefaultCitusShardedClusterExtensions(StackGresShardedCluster cluster) {
    String pgVersion = cluster.getSpec().getPostgres().getVersion();
    StackGresVersion sgVersion = StackGresVersion.getStackGresVersion(cluster);
    Component pgComponent = StackGresComponent.POSTGRESQL.getOrThrow(sgVersion);
    String pgMajorVersion = pgComponent
        .getMajorVersion(pgVersion);
    long pgMajorVersionIndex = pgComponent
        .streamOrderedMajorVersions()
        .zipWithIndex()
        .filter(t -> t.v1.equals(pgMajorVersion))
        .map(Tuple2::v2)
        .findAny()
        .get();
    long pg14Index = pgComponent
        .streamOrderedMajorVersions()
        .zipWithIndex()
        .filter(t -> t.v1.equals("14"))
        .map(Tuple2::v2)
        .findAny()
        .get();
    return List.of(
        pgMajorVersionIndex <= pg14Index
        ? new ExtensionTuple("citus", "12.1-1")
            : new ExtensionTuple("citus", "11.3-1"),
        new ExtensionTuple("citus_columnar", "11.3-1"));
  }

  static List<ExtensionTuple> getDefaultDdpShardedClusterExtensions(StackGresShardedCluster cluster) {
    return List.of(
        new ExtensionTuple("dblink"),
        new ExtensionTuple("postgres_fdw"));
  }

  static List<ExtensionTuple> getDefaultShardingSphereShardedClusterExtensions(StackGresShardedCluster cluster) {
    return List.of(
        new ExtensionTuple("dblink"),
        new ExtensionTuple("postgres_fdw"));
  }

  static List<ExtensionTuple> getDefaultDistributedLogsExtensions(
      StackGresDistributedLogs cluster) {
    return getDefaultDistributedLogsExtensions(
        StackGresDistributedLogsUtil.POSTGRESQL_VERSION,
        StackGresVersion.getStackGresVersion(cluster));
  }

  static List<ExtensionTuple> getDefaultDistributedLogsExtensions(
      String pgVersion, StackGresVersion sgVersion) {
    return Seq.seq(getDefaultClusterExtensions(
        sgVersion,
        pgVersion,
        StackGresPostgresFlavor.VANILLA.toString())).append(
            new ExtensionTuple("timescaledb", "1.7.4"))
        .toList();
  }

  static boolean isLocked(HasMetadata resource) {
    return isLocked(resource, System.currentTimeMillis() / 1000);
  }

  static boolean isLocked(HasMetadata resource, long checkTimestamp) {
    return Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .filter(annotations -> annotations.containsKey(LOCK_POD_KEY)
            && annotations.containsKey(LOCK_TIMEOUT_KEY))
        .map(annotations -> {
          try {
            return Long.parseLong(annotations.get(LOCK_TIMEOUT_KEY));
          } catch (NumberFormatException ex) {
            return null;
          }
        })
        .map(lockTimeout -> checkTimestamp < lockTimeout)
        .orElse(false);
  }

  static boolean isLockedBy(HasMetadata resource, String lockPodName) {
    return isLockedBy(resource, lockPodName,
        System.currentTimeMillis() / 1000);
  }

  static boolean isLockedBy(HasMetadata resource, String lockPodName,
      long checkTimestamp) {
    return Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .filter(annotation -> annotation.containsKey(LOCK_POD_KEY)
            && annotation.containsKey(LOCK_TIMEOUT_KEY))
        .filter(annotations -> annotations.get(LOCK_POD_KEY).equals(lockPodName))
        .map(annotations -> {
          try {
            return Long.parseLong(annotations.get(LOCK_TIMEOUT_KEY));
          } catch (NumberFormatException ex) {
            return null;
          }
        })
        .map(lockTimeout -> checkTimestamp < lockTimeout)
        .orElse(false);
  }

  static void setLock(HasMetadata resource, String lockServiceAccount,
      String lockPodName, long lockDuration) {
    setLock(resource, lockServiceAccount, lockPodName, lockDuration,
        System.currentTimeMillis() / 1000);
  }

  static void setLock(HasMetadata resource, String lockServiceAccount, String lockPodName,
      long lockDuration, long lockTimestamp) {
    resource.setMetadata(
        new ObjectMetaBuilder(resource.getMetadata())
        .removeFromAnnotations(LOCK_SERVICE_ACCOUNT_KEY)
        .removeFromAnnotations(LOCK_POD_KEY)
        .removeFromAnnotations(LOCK_TIMEOUT_KEY)
        .addToAnnotations(LOCK_SERVICE_ACCOUNT_KEY, lockServiceAccount)
        .addToAnnotations(LOCK_POD_KEY, lockPodName)
        .addToAnnotations(LOCK_TIMEOUT_KEY, Long.toString(lockTimestamp + lockDuration))
        .build());
  }

  static void resetLock(HasMetadata resource) {
    resource.setMetadata(
        new ObjectMetaBuilder(resource.getMetadata())
        .removeFromAnnotations(LOCK_SERVICE_ACCOUNT_KEY)
        .removeFromAnnotations(LOCK_POD_KEY)
        .removeFromAnnotations(LOCK_TIMEOUT_KEY)
        .build());
  }

  static String getLockServiceAccount(HasMetadata resource) {
    return Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(LOCK_SERVICE_ACCOUNT_KEY))
        .orElseThrow(() -> new IllegalArgumentException(
            "Resource not locked or locked and annotation "
                + LOCK_SERVICE_ACCOUNT_KEY + " not set"));
  }

  static String getPatroniVersion(StackGresCluster cluster) {
    return getPatroniVersion(cluster, cluster.getSpec().getPostgres().getVersion());
  }

  static String getPatroniVersion(StackGresCluster cluster, String postgresVersion) {
    Component postgresComponentFlavor = getPostgresFlavorComponent(cluster).get(cluster);
    return StackGresComponent.PATRONI.get(cluster).getVersion(
        StackGresComponent.LATEST,
        Map.of(postgresComponentFlavor,
            postgresVersion));
  }

  static String getPatroniVersion(StackGresDistributedLogs distributedLogs) {
    Component postgresComponentFlavor = StackGresComponent.POSTGRESQL.get(distributedLogs);
    return StackGresComponent.PATRONI.get(distributedLogs).getVersion(
        StackGresComponent.LATEST,
        Map.of(postgresComponentFlavor,
            DISTRIBUTEDLOGS_POSTGRES_VERSION));
  }

  static String getLatestPatroniVersion() {
    return StackGresComponent.PATRONI.getLatest().getLatestVersion();
  }

  static String getPatroniImageName(StackGresCluster cluster) {
    return getPatroniImageName(cluster, cluster.getSpec().getPostgres().getVersion());
  }

  static String getPatroniImageName(StackGresCluster cluster, String postgresVersion) {
    Component postgresComponentFlavor = getPostgresFlavorComponent(cluster).get(cluster);
    return StackGresComponent.PATRONI.get(cluster).getImageName(
        StackGresComponent.LATEST,
        Map.of(postgresComponentFlavor,
            postgresVersion));
  }

  static String getPatroniImageName(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.PATRONI.get(distributedLogs).getImageName(
        StackGresComponent.LATEST,
        Map.of(StackGresComponent.POSTGRESQL.get(distributedLogs),
            DISTRIBUTEDLOGS_POSTGRES_VERSION));
  }

  static String getPatroniImageName(StackGresShardedCluster cluster) {
    return getPatroniImageName(cluster, cluster.getSpec().getPostgres().getVersion());
  }

  static String getPatroniImageName(StackGresShardedCluster cluster, String postgresVersion) {
    Component postgresComponentFlavor = getPostgresFlavorComponent(cluster).get(cluster);
    return StackGresComponent.PATRONI.get(cluster).getImageName(
        StackGresComponent.LATEST,
        Map.of(postgresComponentFlavor,
            postgresVersion));
  }

  static @NotNull StackGresComponent getPostgresFlavorComponent(StackGresCluster cluster) {
    return getPostgresFlavorComponent(cluster.getSpec().getPostgres().getFlavor());
  }

  static @NotNull StackGresComponent getPostgresFlavorComponent(StackGresShardedCluster cluster) {
    return getPostgresFlavorComponent(cluster.getSpec().getPostgres().getFlavor());
  }

  static @NotNull StackGresComponent getPostgresFlavorComponent(@Nullable String flavor) {
    final StackGresPostgresFlavor postgresFlavor = getPostgresFlavor(flavor);
    return switch (postgresFlavor) {
      case VANILLA -> StackGresComponent.POSTGRESQL;
      case BABELFISH -> StackGresComponent.BABELFISH;
      default -> throw new IllegalArgumentException("Unknown flavor " + postgresFlavor);
    };
  }

  static StackGresPostgresFlavor getPostgresFlavor(@NotNull StackGresCluster cluster) {
    return getPostgresFlavor(cluster.getSpec().getPostgres().getFlavor());
  }

  static StackGresPostgresFlavor getPostgresFlavor(@NotNull StackGresShardedCluster cluster) {
    return getPostgresFlavor(cluster.getSpec().getPostgres().getFlavor());
  }

  static StackGresPostgresFlavor getPostgresFlavor(@Nullable String flavor) {
    return Optional.ofNullable(flavor)
        .map(value -> {
          try {
            return StackGresPostgresFlavor.fromString(value);
          } catch (IllegalArgumentException e) {
            return null;
          }
        })
        .orElse(StackGresPostgresFlavor.VANILLA);
  }

  /**
   * This is a best-effort to parse the /etc/resolv.conf file and get the search path of K8s.
   */
  static String domainSearchPath() {
    return ResolvConfResolverConfig.CACHED_DOMAIN_SEARCH_PATH;
  }

  /**
   * Convert a {@code Map<String, String>} to a plain Postgres configuration.
   */
  static String toPlainPostgresConfig(Map<String, String> source) {
    return Seq.seq(source.entrySet())
        .map(e -> e.getKey() + "='" + e.getValue().replaceAll("'", "''") + "'")
        .toString("\n");
  }

  /**
   * Convert a plain Postgres configuration to a {@code Map<String, String>}.
   */
  static Map<String, String> fromPlainPostgresConfig(String postgresqlConf) {
    return Seq.of(postgresqlConf.split("\n"))
        .filter(line -> !EMPTY_LINE_PATTERN.matcher(line).matches())
        .map(Tuple::tuple)
        .map(t -> t.concat(PARAMETER_PATTERN.matcher(t.v1)))
        .peek(t -> {
          if (!t.v2.matches()) {
            throw new IllegalArgumentException(
                "Line " + t.v1 + " does not match PostgreSQL's configuration format.");
          }
        })
        .map(Tuple2::v2)
        .filter(Matcher::matches)
        .collect(ImmutableMap.toImmutableMap(
            matcher -> matcher.group("parameter"),
            matcher -> Optional.ofNullable(matcher.group("quoted"))
                .map(quoted -> quoted.replaceAll("[\\']'", "'"))
                .orElseGet(() -> matcher.group("unquoted"))));
  }

  static String getDefaultPullPolicy() {
    return StackGresProperty.SG_IMAGE_PULL_POLICY.get()
        .orElse("IfNotPresent");
  }

  static String getRestapiImageNameWithTag(ConfigContext context) {
    return getImageNameWithTag(
        context,
        Optional.of(context.getConfig().getSpec())
        .map(StackGresConfigSpec::getRestapi)
        .map(StackGresConfigRestapi::getImage),
        "stackgres/restapi");
  }

  static String getAdminuiImageNameWithTag(ConfigContext context) {
    return getImageNameWithTag(
        context,
        Optional.of(context.getConfig().getSpec())
        .map(StackGresConfigSpec::getAdminui)
        .map(StackGresConfigAdminui::getImage),
        "stackgres/admin-ui");
  }

  static String getJobsImageNameWithTag(ConfigContext context) {
    return getImageNameWithTag(
        context,
        Optional.of(context.getConfig().getSpec())
        .map(StackGresConfigSpec::getJobs)
        .map(StackGresConfigJobs::getImage),
        "stackgres/jobs");
  }
      
  static String getImageNameWithTag(
      ConfigContext context,
      Optional<StackGresConfigImage> image,
      String defaultImageName) {
    final String containerRegistry = context.getConfig().getSpec().getContainerRegistry();
    String imageName = image
        .map(StackGresConfigImage::getName)
        .orElse(defaultImageName);
    String imageTag = image
        .map(StackGresConfigImage::getTag)
        .or(() -> StackGresProperty.OPERATOR_IMAGE_VERSION.get())
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not determine the image tag."
                + " Missing OPERATOR_IMAGE_VERSION environment variable"));
    return IMAGE_NAME_WITH_REGISTRY_PATTERN.matcher(imageName).matches()
        ? imageName + ":" + imageTag
            : containerRegistry + "/" + imageName + ":" + imageTag;
  }

}
