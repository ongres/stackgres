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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.WebClientFactory.WebClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
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

  private final Map<URI, ExtensionMetadataCache> uriCache =
      new HashMap<>();

  private final WebClientFactory webClientFactory;
  private final List<URI> extensionsRepositoryUris;

  public ExtensionMetadataManager(WebClientFactory webClientFactory,
                                  List<URI> extensionsRepositoryUrls) {
    this.webClientFactory = webClientFactory;
    this.extensionsRepositoryUris = extensionsRepositoryUrls;
  }

  public URI getExtensionRepositoryUri(URI extensionsRepositoryUri) {
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
        .map(this::extractLatestVersions)
        .orElse(ImmutableList.of());
  }

  public Optional<StackGresExtensionMetadata> findExtensionCandidateAnyVersion(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return getExtensionsAnyVersion(cluster, extension, detectOs).stream()
        .findFirst();
  }

  public List<StackGresExtensionMetadata> requestExtensionsAnyVersion(
      ExtensionRequest extensionRequest,
      boolean detectOs) {
    return Optional
        .ofNullable(
            getExtensionsMetadata().indexAnyVersions.get(
                StackGresExtensionIndexAnyVersion.fromClusterExtension(extensionRequest, detectOs)
            )
        )
        .map(this::extractLatestVersions)
        .orElse(List.of());
  }

  public List<StackGresExtensionMetadata> getExtensionsAnyVersion(
      StackGresCluster cluster, StackGresClusterExtension extension, boolean detectOs) {
    return Optional
        .ofNullable(getExtensionsMetadata().indexAnyVersions
            .get(StackGresExtensionIndexAnyVersion
                .fromClusterExtension(cluster, extension, detectOs)))
        .map(this::extractLatestVersions)
        .orElse(List.of());
  }

  private List<StackGresExtensionMetadata> extractLatestVersions(
      List<StackGresExtensionMetadata> list) {
    return Seq.seq(list)
        .map(e -> Tuple.tuple(e.getVersion().getVersion(), e.getMajorBuild(), e))
        .grouped(Tuple3::limit2)
        .map(group -> group.v2
            .map(Tuple3::v3)
            .min(StackGresExtensionMetadata::compareBuild)
            .orElseThrow())
        .collect(Collectors.toUnmodifiableList());
  }

  public Collection<StackGresExtensionMetadata> getExtensions() {
    return getExtensionsMetadata().index.values();
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "False positive")
  private synchronized ExtensionMetadataCache getExtensionsMetadata() {
    boolean updated = false;
    for (URI extensionsRepositoryUri : extensionsRepositoryUris) {
      try {
        final Duration cacheTimeout =
            getUriQueryParameter(
                extensionsRepositoryUri, CACHE_TIMEOUT_PARAMETER)
                .map(Duration::parse)
                .orElse(Duration.of(1, ChronoUnit.HOURS));
        if (Optional.ofNullable(uriCache.get(extensionsRepositoryUri))
            .map(ExtensionMetadataCache::getCreated)
            .orElse(Instant.MIN)
            .plus(cacheTimeout)
            .isBefore(Instant.now())) {
          try (WebClient client = webClientFactory.create(extensionsRepositoryUri)) {
            LOGGER.info("Downloading extensions metadata from {}", extensionsRepositoryUri);
            final URI indexUri = ExtensionUtil.getIndexUri(extensionsRepositoryUri);
            StackGresExtensions repositoryExtensions = client.getJson(
                indexUri, StackGresExtensions.class);
            ExtensionMetadataCache current = ExtensionMetadataCache.from(
                extensionsRepositoryUri, repositoryExtensions);
            uriCache.put(extensionsRepositoryUri, current);
            updated = true;
          }
        }
      } catch (Exception ex) {
        String message = "Can not download extensions metadata from "
            + extensionsRepositoryUri;
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
      uriCache.put(LATEST_MERGED_CACHE_URI, mergedCache);
      return mergedCache;
    }

    return uriCache.get(LATEST_MERGED_CACHE_URI);
  }

  public StackGresExtensionPublisher getPublisher(String publisher) {
    return Optional.ofNullable(getExtensionsMetadata().publishers.get(publisher))
        .orElseThrow(() -> new RuntimeException("Publisher " + publisher + " was not found"));
  }

  static class ExtensionMetadataCache {
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

    void merge(ExtensionMetadataCache other) {
      index.putAll(other.index);
      indexSameMajorBuilds.putAll(other.indexSameMajorBuilds);
      indexAnyVersions.putAll(other.indexAnyVersions);
      publishers.putAll(other.publishers);
    }
  }

}
