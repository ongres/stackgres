/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.WebClientFactory.WebClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExtensionMetadataManager {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExtensionMetadataManager.class);

  private static final String SKIP_HOSTNAME_VERIFICATION_PARAMETER = "skipHostnameVerification";

  private static final Object EXTENSIONS = new Object();

  public static final String SHA256_SUFFIX = ".sha256";
  public static final String TGZ_SUFFIX = ".tgz";
  public static final String INSTALLED_SUFFIX = ".installed";
  public static final String PENDING_SUFFIX = ".pending";

  private final Cache<Object, ExtensionMetadataCache>
      extensionsMetadataCache = CacheBuilder.newBuilder()
          .expireAfterWrite(Duration.of(1, ChronoUnit.HOURS))
          .initialCapacity(1).maximumSize(1).build();
  private final Map<URI, ExtensionMetadataCache> uriCache =
      new HashMap<>();

  private final WebClientFactory webClientFactory;
  private final List<URI> extensionsRepositoryUris;

  public ExtensionMetadataManager(WebClientFactory webClientFactory,
      List<URI> extensionsRepositoryUrls) {
    this.webClientFactory = webClientFactory;
    this.extensionsRepositoryUris = extensionsRepositoryUrls;
  }

  public StackGresExtensionMetadata getExtensionCandidate(
      StackGresClusterInstalledExtension installedExtension) throws Exception {
    return Optional
        .ofNullable(getExtensionsMetadata().index
            .get(new StackGresExtensionIndex(installedExtension)))
        .orElseThrow(
            () -> new IllegalArgumentException("Can not find version of extension "
                + ExtensionUtil.getDescription(installedExtension)));
  }

  public StackGresExtensionMetadata getExtensionCandidateSameMajorBuild(
      StackGresCluster cluster, StackGresClusterExtension extension) throws Exception {
    return getExtensionsSameMajorBuild(cluster, extension).stream()
        .sorted((l, r) -> r.compareBuild(l))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Can not find candidate version of extension "
                + ExtensionUtil.getDescription(cluster, extension)));
  }

  public List<StackGresExtensionMetadata> getExtensionsSameMajorBuild(
      StackGresCluster cluster, StackGresClusterExtension extension) throws Exception {
    return Optional
        .ofNullable(getExtensionsMetadata().indexSameMajorBuilds
            .get(new StackGresExtensionIndexSameMajorBuild(cluster, extension)))
        .orElse(ImmutableList.of());
  }

  public StackGresExtensionMetadata getExtensionCandidateAnyVersion(
      StackGresCluster cluster, StackGresClusterExtension extension) throws Exception {
    return getExtensionsAnyVersion(cluster, extension).stream()
        .sorted((l, r) -> r.compareBuild(l))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Can not find candidate for any version"
                + " of extension " + ExtensionUtil.getDescription(cluster, extension)));
  }

  public List<StackGresExtensionMetadata> getExtensionsAnyVersion(
      StackGresCluster cluster, StackGresClusterExtension extension) throws Exception {
    return Optional
        .ofNullable(getExtensionsMetadata().indexAnyVersions
            .get(new StackGresExtensionIndexAnyVersion(cluster, extension)))
        .orElse(ImmutableList.of());
  }

  public Collection<StackGresExtensionMetadata> getExtensions() throws Exception {
    return getExtensionsMetadata().index.values();
  }

  private ExtensionMetadataCache getExtensionsMetadata()
      throws Exception {
    return extensionsMetadataCache.get(EXTENSIONS, this::downloadExtensionsMetadata);
  }

  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
      justification = "False positive")
  private ExtensionMetadataCache downloadExtensionsMetadata()
      throws Exception {
    ExtensionMetadataCache cache = new ExtensionMetadataCache(
        new HashMap<>(), new HashMap<>(), new HashMap<>());
    for (URI extensionsRepositoryUri : extensionsRepositoryUris) {
      try {
        LOGGER.info("Downloading extensions metadata from {}", extensionsRepositoryUri);
        boolean skipHostnameVerification =
            ExtensionUtil.getUriQueryParameter(
                extensionsRepositoryUri, SKIP_HOSTNAME_VERIFICATION_PARAMETER)
            .map(Boolean::valueOf).orElse(false);
        final URI indexUri = ExtensionUtil.getIndexUri(extensionsRepositoryUri);
        try (WebClient client = webClientFactory.create(skipHostnameVerification)) {
          StackGresExtensions repositoryExtensions = client.getJson(
              indexUri, StackGresExtensions.class);
          ExtensionMetadataCache current = ExtensionMetadataCache.from(
              extensionsRepositoryUri, repositoryExtensions);
          cache.merge(current);
          uriCache.put(extensionsRepositoryUri, current);
        }
      } catch (Exception ex) {
        ExtensionMetadataCache previous = uriCache.get(extensionsRepositoryUri);
        if (previous != null) {
          cache.merge(previous);
        }
        LOGGER.error("Can not download extensions metadata from {}", extensionsRepositoryUri, ex);
      }
    }
    return cache;
  }

  static class ExtensionMetadataCache {
    final Map<StackGresExtensionIndex, StackGresExtensionMetadata> index;
    final Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>>
        indexSameMajorBuilds;
    final Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>>
        indexAnyVersions;

    static ExtensionMetadataCache from(URI repositoryUri, StackGresExtensions extensions) {
      return new ExtensionMetadataCache(
          ExtensionUtil.toExtensionsMetadataIndex(repositoryUri, extensions),
          ExtensionUtil.toExtensionsMetadataIndexSameMajorBuilds(repositoryUri, extensions),
          ExtensionUtil.toExtensionsMetadataIndexAnyVersions(repositoryUri, extensions));
    }

    ExtensionMetadataCache(
        Map<StackGresExtensionIndex, StackGresExtensionMetadata> index,
        Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>>
            indexSameMajorBuilds,
        Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>>
            indexAnyVersions) {
      this.index = index;
      this.indexSameMajorBuilds = indexSameMajorBuilds;
      this.indexAnyVersions = indexAnyVersions;
    }

    void merge(ExtensionMetadataCache other) {
      index.putAll(other.index);
      indexSameMajorBuilds.putAll(other.indexSameMajorBuilds);
      indexAnyVersions.putAll(other.indexAnyVersions);
    }
  }

}
