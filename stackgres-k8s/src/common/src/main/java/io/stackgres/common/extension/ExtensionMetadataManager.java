/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static io.stackgres.common.WebClientFactory.getUriQueryParameter;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.WebClientFactory.WebClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import jakarta.ws.rs.core.UriBuilder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExtensionMetadataManager {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtensionMetadataManager.class);

  private static final URI LATEST_MERGED_CACHE_URI = URI.create("cache://merged-cache");

  private static final String CACHE_TIMEOUT_PARAMETER = "cacheTimeout";

  private static final String CACHE_REFRESH_DISABLED_PARAMETER = "cacheRefreshDisabled";

  private static final Duration DEFAULT_CACHE_TIMEOUT = Duration.of(7, ChronoUnit.DAYS);

  private static final Duration MINIMUM_CACHE_TIMEOUT = Duration.of(1, ChronoUnit.HOURS);

  private final Map<URI, ExtensionMetadataCache> uriCache =
      new HashMap<>();

  private final WebClientFactory webClientFactory;
  private final Supplier<List<URI>> extensionsRepositoryUrisSupplier;
  private final Supplier<Map<String, String>> headersProvider;

  protected ExtensionMetadataManager(
      WebClientFactory webClientFactory,
      List<URI> extensionsRepositoryUrls,
      Supplier<Map<String, String>> headersProvider) {
    this(webClientFactory, () -> extensionsRepositoryUrls, headersProvider);
  }

  protected ExtensionMetadataManager(
      WebClientFactory webClientFactory,
      Supplier<List<URI>> extensionsRepositoryUrisSupplier,
      Supplier<Map<String, String>> headersProvider) {
    this.webClientFactory = webClientFactory;
    this.extensionsRepositoryUrisSupplier = extensionsRepositoryUrisSupplier;
    this.headersProvider = headersProvider;
  }

  public ExtensionMetadataManager() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.webClientFactory = null;
    this.extensionsRepositoryUrisSupplier = null;
    this.headersProvider = null;
  }

  public URI getExtensionRepositoryUri(URI extensionsRepositoryUri) {
    final List<URI> extensionsRepositoryUris = extensionsRepositoryUrisSupplier.get();
    return Seq.seq(extensionsRepositoryUris)
        .filter(anExtensionsRepositoryUri -> anExtensionsRepositoryUri.toString()
            .startsWith(extensionsRepositoryUri.toString()))
        .findFirst()
        .orElseGet(() -> {
          LOGGER.warn("URI {} not found in any configured extensions repository URIs: {}",
              extensionsRepositoryUri, extensionsRepositoryUris);
          return extensionsRepositoryUri;
        });
  }

  public StackGresExtensionMetadata getExtensionCandidateSameMajorBuild(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return findExtensionCandidateSameMajorBuild(cluster, extension, detectOs)
        .orElseThrow(
            () -> new IllegalArgumentException("Can not find candidate version of extension "
                + ExtensionUtil.getDescription(cluster, extension, detectOs)));
  }

  public Optional<StackGresExtensionMetadata> findExtensionCandidateSameMajorBuild(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return getExtensionsSameMajorBuild(cluster, extension, detectOs).stream()
        .findFirst();
  }

  public List<StackGresExtensionMetadata> getExtensionsSameMajorBuild(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return Optional
        .ofNullable(getExtensionsMetadata().indexSameMajorBuilds
            .get(StackGresExtensionIndexSameMajorBuild
                .fromClusterExtension(cluster, extension, detectOs)))
        .map(list -> filterAvailableForPostgresVersion(cluster, list))
        .map(this::extractLatestBuildVersions)
        .orElse(List.of());
  }

  public Optional<StackGresExtensionMetadata> findExtensionCandidateAnyVersion(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return getExtensionsAnyVersion(cluster, extension, detectOs).stream()
        .findFirst();
  }

  public List<StackGresExtensionMetadata> getExtensionsAnyVersion(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return Optional
        .ofNullable(getExtensionsMetadata().indexAnyVersions
            .get(StackGresExtensionIndexAnyVersion
                .fromClusterExtension(cluster, extension, detectOs)))
        .map(list -> filterAvailableForPostgresVersion(cluster, list))
        .map(this::extractLatestBuildVersions)
        .stream()
        .flatMap(List::stream)
        .sorted(Comparator.comparing(
            Function.<StackGresExtensionMetadata>identity()
            .andThen(StackGresExtensionMetadata::getVersion)
            .andThen(StackGresExtensionVersion::getVersion)
            .andThen(StackGresUtil::sortableVersion))
            .reversed())
        .toList();
  }

  /**
   * Remove the builds that require a Postgres minor newer than the one of the cluster
   * (postgresFromVersion) or that only run up to a Postgres minor older than the one of the
   * cluster (postgresToVersion), since the ABI of Postgres may change between minors of the same
   * major. Both bounds are inclusive.
   */
  private List<StackGresExtensionMetadata> filterAvailableForPostgresVersion(
      StackGresCluster cluster, List<StackGresExtensionMetadata> list) {
    final String postgresVersion = StackGresUtil.sortableVersion(
        StackGresUtil.getPostgresFlavorComponent(cluster).get(cluster)
        .getVersion(cluster.getSpec().getPostgres().getVersion()));
    return list.stream()
        .filter(extensionMetadata -> Optional.of(extensionMetadata.getTarget())
            .map(StackGresExtensionVersionTarget::getPostgresFromVersion)
            .map(StackGresUtil::sortableVersion)
            .map(postgresFromVersion -> postgresFromVersion.compareTo(postgresVersion) <= 0)
            .orElse(true))
        .filter(extensionMetadata -> Optional.of(extensionMetadata.getTarget())
            .map(StackGresExtensionVersionTarget::getPostgresToVersion)
            .map(StackGresUtil::sortableVersion)
            .map(postgresToVersion -> postgresToVersion.compareTo(postgresVersion) >= 0)
            .orElse(true))
        .toList();
  }

  private List<StackGresExtensionMetadata> extractLatestBuildVersions(
      List<StackGresExtensionMetadata> list) {
    return Seq.seq(list)
        .map(e -> Tuple.tuple(e.getVersion().getVersion(), e.getMajorBuild(), e))
        .grouped(Tuple3::limit2)
        .map(group -> group.v2
            .map(Tuple3::v3)
            .max(Comparator.comparing(StackGresExtensionMetadata::getBuild))
            .orElseThrow())
        .sorted(Comparator.comparing(
            Function.<StackGresExtensionMetadata>identity()
            .andThen(StackGresExtensionMetadata::getVersion)
            .andThen(StackGresExtensionVersion::getVersion)
            .andThen(StackGresUtil::sortableVersion))
            .reversed())
        .toList();
  }

  public Collection<StackGresExtensionMetadata> getExtensions() {
    return getExtensionsMetadata().index.values();
  }

  synchronized ExtensionMetadataCache getExtensionsMetadata() {
    final List<URI> extensionsRepositoryUris = extensionsRepositoryUrisSupplier.get();
    boolean updated = false;
    for (URI extensionsRepositoryUri : extensionsRepositoryUris) {
      try {
        final Duration cacheTimeout =
            getUriQueryParameter(
                extensionsRepositoryUri, CACHE_TIMEOUT_PARAMETER)
                .map(Duration::parse)
                .map(timeout -> timeout.compareTo(MINIMUM_CACHE_TIMEOUT) < 0
                    ? MINIMUM_CACHE_TIMEOUT : timeout)
                .orElse(DEFAULT_CACHE_TIMEOUT);
        final boolean refreshDisabled =
            getUriQueryParameter(
                extensionsRepositoryUri, CACHE_REFRESH_DISABLED_PARAMETER)
                .map(Boolean::parseBoolean)
                .orElse(false);
        final boolean cacheAbsent = uriCache.get(extensionsRepositoryUri) == null;
        final boolean cacheExpired = Optional.ofNullable(uriCache.get(extensionsRepositoryUri))
            .map(ExtensionMetadataCache::getCreated)
            .orElse(Instant.MIN)
            .plus(cacheTimeout)
            .isBefore(Instant.now());
        if (cacheAbsent || (!refreshDisabled && cacheExpired)) {
          try (WebClient client = webClientFactory.create(extensionsRepositoryUri, headersProvider.get())) {
            LOGGER.info("Downloading extensions metadata from {}",
                WebClientFactory.obfuscateUri(extensionsRepositoryUri));
            final URI indexUri = ExtensionUtil.getIndexUri(extensionsRepositoryUri);
            StackGresExtensions repositoryExtensions = client.getJson(
                indexUri, StackGresExtensions.class);
            ExtensionMetadataCache current = ExtensionMetadataCache.from(
                extensionsRepositoryUri, repositoryExtensions);
            putExtensionMetadataCache(extensionsRepositoryUri, current);
            updated = true;
          }
        }
      } catch (Exception ex) {
        String message = "Can not download extensions metadata from "
            + WebClientFactory.obfuscateUri(extensionsRepositoryUri);
        if (uriCache.get(extensionsRepositoryUri) != null) {
          LOGGER.warn(message, ex);
        } else {
          throw new RuntimeException(message, ex);
        }
      }
    }

    if (updated || extensionsRepositoryUris.isEmpty()) {
      final ExtensionMetadataCache mergedCache = new ExtensionMetadataCache(
          new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
      for (URI extensionsRepositoryUri : extensionsRepositoryUris) {
        mergedCache.merge(uriCache.get(extensionsRepositoryUri));
      }
      putExtensionMetadataCache(LATEST_MERGED_CACHE_URI, mergedCache);
      return mergedCache;
    }

    return uriCache.get(LATEST_MERGED_CACHE_URI);
  }

  protected void putExtensionMetadataCache(
      URI uri,
      ExtensionMetadataCache cache) {
    uriCache.put(uri, cache);
  }

  public StackGresExtensionPublisher getPublisher(String publisher) {
    return Optional.ofNullable(getExtensionsMetadata().publishers.get(publisher))
        .orElseThrow(() -> new RuntimeException("Publisher " + publisher + " was not found"));
  }

  protected static class ExtensionMetadataCache {
    final Instant created;
    final Map<StackGresExtensionIndex, StackGresExtensionMetadata> index;
    final Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>>
        indexSameMajorBuilds;
    final Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>>
        indexAnyVersions;
    final Map<String, StackGresExtensionPublisher> publishers;

    ExtensionMetadataCache(
        Map<StackGresExtensionIndex, StackGresExtensionMetadata> index,
        Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>>
            indexSameMajorBuilds,
        Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>>
            indexAnyVersions,
        Map<String, StackGresExtensionPublisher> publishers) {
      this.created = Instant.now();
      this.index = index;
      this.indexSameMajorBuilds = indexSameMajorBuilds;
      this.indexAnyVersions = indexAnyVersions;
      this.publishers = publishers;
    }

    static ExtensionMetadataCache from(URI repositoryUri, StackGresExtensions extensions) {
      URI repositoryBaseUri = UriBuilder.fromUri(repositoryUri).replaceQuery(null).build();
      return new ExtensionMetadataCache(
          ExtensionUtil.toExtensionsMetadataIndex(repositoryBaseUri, extensions),
          ExtensionUtil.toExtensionsMetadataIndexSameMajorBuilds(repositoryBaseUri, extensions),
          ExtensionUtil.toExtensionsMetadataIndexAnyVersions(repositoryBaseUri, extensions),
          ExtensionUtil.toPublishersIndex(extensions));
    }

    public Instant getCreated() {
      return created;
    }

    public Map<StackGresExtensionIndex, StackGresExtensionMetadata> getIndex() {
      return index;
    }

    public Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>> getIndexSameMajorBuilds() {
      return indexSameMajorBuilds;
    }

    public Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>> getIndexAnyVersions() {
      return indexAnyVersions;
    }

    public Map<String, StackGresExtensionPublisher> getPublishers() {
      return publishers;
    }

    void merge(ExtensionMetadataCache other) {
      index.putAll(other.index);
      indexSameMajorBuilds.putAll(other.indexSameMajorBuilds);
      indexAnyVersions.putAll(other.indexAnyVersions);
      publishers.putAll(other.publishers);
    }
  }

}
