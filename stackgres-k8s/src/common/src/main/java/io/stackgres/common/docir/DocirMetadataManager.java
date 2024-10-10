/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import static io.stackgres.common.WebClientFactory.getUriQueryParameter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.WebClientFactory.WebClient;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;
import io.stackgres.common.docir.model.Addons;
import io.stackgres.common.docir.model.BaseImages;
import io.stackgres.common.docir.model.Extensions;
import io.stackgres.common.docir.model.ImageUrl;
import io.stackgres.common.docir.model.ImageUrlRequest;
import io.stackgres.common.docir.model.Versions;
import io.stackgres.common.extension.ExtensionUtil;
import jakarta.ws.rs.core.UriBuilder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client of the StackGres docir REST API that caches the metadata (base images, flavors, addons
 * and extensions) of each docir repository and resolves the images to use for a cluster.
 *
 * <p>The default repository is the one configured in {@code SGConfig.spec.repository.url} while a
 * cluster may override it with {@code SGCluster.spec.configurations.registry.url}. Each application
 * (operator, REST API and cluster controller) provides the default repository URL and the HTTP
 * headers (User-Agent) through its own subclass.</p>
 *
 * <p>When the metadata JSON files are available as classpath resources (see the test resources)
 * they are used instead of the REST API, unless {@code onlyLoadFromApi} is set.</p>
 */
public abstract class DocirMetadataManager {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DocirMetadataManager.class);

  public static final URI LOCAL_RESOURCES_URI = URI.create("resources:/");

  private static final String CACHE_TIMEOUT_PARAMETER = "cacheTimeout";

  private final Map<URI, DocirFlavorMetadataCache> flavorUriCache =
      new HashMap<>();

  private final Map<URI, DocirExtensionMetadataCache> extensionsUriCache =
      new HashMap<>();

  private final Map<URI, DocirImageMetadataCache> imageUriCache =
      new HashMap<>();

  private final ObjectMapper objectMapper;
  private final WebClientFactory webClientFactory;
  private final Supplier<URI> defaultRepositoryUriSupplier;
  private final Supplier<Map<String, String>> headersSupplier;
  private final boolean onlyLoadFromApi;

  protected DocirMetadataManager(
      ObjectMapper objectMapper,
      WebClientFactory webClientFactory,
      Supplier<URI> defaultRepositoryUriSupplier,
      Supplier<Map<String, String>> headersSupplier,
      boolean onlyLoadFromApi) {
    this.objectMapper = objectMapper;
    this.webClientFactory = webClientFactory;
    this.defaultRepositoryUriSupplier = defaultRepositoryUriSupplier;
    this.headersSupplier = headersSupplier;
    this.onlyLoadFromApi = onlyLoadFromApi;
  }

  public DocirMetadataManager() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.objectMapper = null;
    this.webClientFactory = null;
    this.defaultRepositoryUriSupplier = null;
    this.headersSupplier = null;
    this.onlyLoadFromApi = false;
  }

  ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  WebClientFactory getWebClientFactory() {
    return webClientFactory;
  }

  /**
   * The docir repository configured in {@code SGConfig.spec.repository.url}.
   */
  public URI getDefaultRepositoryUri() {
    return defaultRepositoryUriSupplier.get();
  }

  /**
   * The docir repository to use: {@code repositoryUri} when not null, the default one otherwise.
   */
  public URI getRepositoryUri(URI repositoryUri) {
    return Optional.ofNullable(repositoryUri).orElseGet(this::getDefaultRepositoryUri);
  }

  /**
   * The docir repository to use for the cluster (see
   * {@code SGCluster.spec.configurations.registry.url}).
   */
  public URI getRepositoryUri(StackGresCluster cluster) {
    return getRepositoryUri(StackGresUtil.getRegistryUri(cluster).orElse(null));
  }

  private WebClient createWebClient(URI uri) throws Exception {
    return getWebClientFactory().create(uri, headersSupplier.get());
  }

  private boolean loadFlavorsFromApi() {
    return onlyLoadFromApi
        || !hasResource(getBaseImagesResourceUrl())
        || !hasResource(getAddonsResourceUrl())
        || DocirUtil.SUPPORTED_FLAVORS.stream()
        .noneMatch(flavor -> hasResource(getVersionsResourceUrl(flavor)));
  }

  private URI getFlavorsCacheUri(URI repositoryUri) {
    return loadFlavorsFromApi() ? getRepositoryUri(repositoryUri) : LOCAL_RESOURCES_URI;
  }

  public Collection<DocirFlavorMetadata> getFlavors() {
    return getFlavors((URI) null);
  }

  public Collection<DocirFlavorMetadata> getFlavors(URI repositoryUri) {
    return getFlavorsMetadata(repositoryUri).index.values();
  }

  public List<DocirAddon> getAddons(URI repositoryUri) {
    return getFlavorsMetadata(repositoryUri).addons;
  }

  /**
   * The latest version of the addon built on the base image for the platform, see
   * {@link DocirUtil#findLatestAddonVersion(List, String, DocirBase, String, String)}.
   */
  public Optional<DocirAddonVersion> findLatestAddonVersion(
      URI repositoryUri, String addon, DocirBase base, String os, String arch) {
    return DocirUtil.findLatestAddonVersion(getAddons(repositoryUri), addon, base, os, arch);
  }

  synchronized DocirFlavorMetadataCache getFlavorsMetadata(URI repositoryUri) {
    final boolean loadFromApi = loadFlavorsFromApi();
    final URI cacheUri = getFlavorsCacheUri(repositoryUri);
    try {
      if (loadFromApi) {
        updateFlavorCacheFromApi(cacheUri);
      } else {
        updateFlavorCacheFromLocalResources(cacheUri);
      }
    } catch (Exception ex) {
      String message = "Can not load flavors, base images and addons metadata from "
          + WebClientFactory.obfuscateUri(cacheUri);
      if (flavorUriCache.get(cacheUri) != null) {
        LOGGER.warn(message, ex);
      } else {
        throw new RuntimeException(message, ex);
      }
    }
    return flavorUriCache.get(cacheUri);
  }

  private boolean isCacheExpired(URI repositoryUri, Instant created) {
    final Duration cacheTimeout =
        getUriQueryParameter(
            repositoryUri, CACHE_TIMEOUT_PARAMETER)
            .map(Duration::parse)
            .orElse(Duration.of(1, ChronoUnit.HOURS));
    return Optional.ofNullable(created)
        .orElse(Instant.MIN)
        .plus(cacheTimeout)
        .isBefore(Instant.now());
  }

  private void updateFlavorCacheFromApi(URI repositoryUri) throws Exception {
    if (!isCacheExpired(repositoryUri, Optional.ofNullable(flavorUriCache.get(repositoryUri))
        .map(DocirFlavorMetadataCache::getCreated).orElse(null))) {
      return;
    }
    try (WebClient client = createWebClient(repositoryUri)) {
      LOGGER.info("Downloading flavors, base images and addons metadata from {}",
          WebClientFactory.obfuscateUri(repositoryUri));
      final String repository = repositoryUri.toASCIIString();
      final BaseImages repositoryBases = client.getJson(
          DocirUtil.getBaseImagesApiUri(repositoryUri), BaseImages.class);
      final List<DocirBase> bases = Optional.ofNullable(repositoryBases.baseImages())
          .map(foundBaseImages -> DocirUtil.asDocirBases(repository, foundBaseImages))
          .orElse(List.of());
      final Addons repositoryAddons = client.getJson(
          DocirUtil.getAddonsApiUri(repositoryUri), Addons.class);
      final List<DocirAddon> addons = Optional.ofNullable(repositoryAddons.addons())
          .map(foundAddons -> DocirUtil.asDocirAddons(repository, foundAddons))
          .orElse(List.of());
      final ArrayList<DocirFlavor> flavors = new ArrayList<>();
      for (var flavor : DocirUtil.SUPPORTED_FLAVORS) {
        final Versions repositoryVersions = client.getJson(
            UriBuilder.fromUri(DocirUtil.getVersionsApiUri(repositoryUri))
            .queryParam("flavor", flavor)
            .queryParam("tshirt-size", DocirUtil.DEFAULT_TSHIRT_SIZE)
            .build(), Versions.class);
        flavors.addAll(Optional.ofNullable(repositoryVersions.versions())
            .map(foundVersions -> DocirUtil.asDocirFlavors(repository, flavor, foundVersions))
            .orElse(List.of()));
      }
      flavorUriCache.put(repositoryUri, DocirFlavorMetadataCache.from(flavors, bases, addons));
    }
  }

  private void updateFlavorCacheFromLocalResources(URI repositoryUri) throws IOException {
    if (!isCacheExpired(repositoryUri, Optional.ofNullable(flavorUriCache.get(repositoryUri))
        .map(DocirFlavorMetadataCache::getCreated).orElse(null))) {
      return;
    }
    LOGGER.info("Reading flavors, base images and addons metadata from local resources");
    final String repository = repositoryUri.toASCIIString();
    final List<DocirBase> bases;
    try (var stream = getResourceAsStream(getBaseImagesResourceUrl())) {
      var repositoryBases = getObjectMapper().readValue(stream, BaseImages.class);
      bases = Optional.ofNullable(repositoryBases.baseImages())
          .map(foundBaseImages -> DocirUtil.asDocirBases(repository, foundBaseImages))
          .orElse(List.of());
    }
    final List<DocirAddon> addons;
    try (var stream = getResourceAsStream(getAddonsResourceUrl())) {
      var repositoryAddons = getObjectMapper().readValue(stream, Addons.class);
      addons = Optional.ofNullable(repositoryAddons.addons())
          .map(foundAddons -> DocirUtil.asDocirAddons(repository, foundAddons))
          .orElse(List.of());
    }
    final ArrayList<DocirFlavor> flavors = new ArrayList<>();
    for (var flavor : DocirUtil.SUPPORTED_FLAVORS) {
      if (!hasResource(getVersionsResourceUrl(flavor))) {
        continue;
      }
      try (var stream = getResourceAsStream(getVersionsResourceUrl(flavor))) {
        var repositoryVersions = getObjectMapper().readValue(stream, Versions.class);
        flavors.addAll(Optional.ofNullable(repositoryVersions.versions())
            .map(foundVersions -> DocirUtil.asDocirFlavors(repository, flavor, foundVersions))
            .orElse(List.of()));
      }
    }
    flavorUriCache.put(repositoryUri, DocirFlavorMetadataCache.from(flavors, bases, addons));
  }

  public DocirExtensionMetadata getExtensionCandidateSameMajorBuild(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    return findExtensionCandidateSameMajorBuild(context, cluster, extension, detectOs)
        .orElseThrow(
            () -> new IllegalArgumentException("Can not find candidate version of extension "
                + ExtensionUtil.getDescription(cluster, extension, detectOs)));
  }

  public Optional<DocirExtensionMetadata> findExtensionCandidateSameMajorBuild(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    return getExtensionsSameMajorBuild(context, cluster, extension, detectOs)
        .stream()
        .findFirst();
  }

  public List<DocirExtensionMetadata> getExtensionsSameMajorBuild(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    return Optional
        .ofNullable(getExtensionsMetadata(getRepositoryUri(cluster)).indexSameMajorBuilds
            .get(DocirExtensionIndexSameMajorRevision
                .fromClusterExtension(context, cluster, extension, detectOs)))
        .orElse(List.of());
  }

  public Optional<DocirExtensionMetadata> findExtensionCandidateAnyVersion(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    return getExtensionsAnyVersion(context, cluster, extension, detectOs)
        .stream()
        .findFirst();
  }

  public List<DocirExtensionMetadata> getExtensionsAnyVersion(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    return Optional
        .ofNullable(getExtensionsMetadata(getRepositoryUri(cluster)).indexAnyVersions
            .get(DocirExtensionIndexAnyVersion
                .fromClusterExtension(context, cluster, extension, detectOs)))
        .orElse(List.of());
  }

  public Collection<DocirExtensionMetadata> getExtensions() {
    return getExtensions((URI) null);
  }

  public Collection<DocirExtensionMetadata> getExtensions(URI repositoryUri) {
    return getExtensionsMetadata(repositoryUri).index.values();
  }

  synchronized DocirExtensionMetadataCache getExtensionsMetadata(URI repositoryUri) {
    final Collection<DocirFlavorMetadata> flavors = Seq.seq(getFlavors(repositoryUri))
        .grouped(Function.<DocirFlavorMetadata>identity()
            .andThen(DocirFlavorMetadata::getFlavor)
            .andThen(flavor -> Tuple.tuple(flavor.getName(), flavor.getMajor(), flavor.getMinor())))
        .map(group -> group.v2.findFirst().get())
        .toList();
    final boolean loadFromApi = onlyLoadFromApi || flavors.stream()
        .noneMatch(supported -> hasResource(getExtensionsResourceUrl(supported)));
    final URI cacheUri = loadFromApi ? getRepositoryUri(repositoryUri) : LOCAL_RESOURCES_URI;
    try {
      if (loadFromApi) {
        updateExtensionsCacheFromApi(flavors, cacheUri);
      } else {
        updateExtensionsCacheFromLocalResources(flavors, cacheUri);
      }
    } catch (Exception ex) {
      String message = "Can not load extensions metadata from "
          + WebClientFactory.obfuscateUri(cacheUri);
      if (extensionsUriCache.get(cacheUri) != null) {
        LOGGER.warn(message, ex);
      } else {
        throw new RuntimeException(message, ex);
      }
    }
    return extensionsUriCache.get(cacheUri);
  }

  private void updateExtensionsCacheFromApi(
      Collection<DocirFlavorMetadata> flavors, URI repositoryUri) throws Exception {
    if (!isCacheExpired(repositoryUri, Optional.ofNullable(extensionsUriCache.get(repositoryUri))
        .map(DocirExtensionMetadataCache::getCreated).orElse(null))) {
      return;
    }
    try (WebClient client = createWebClient(repositoryUri)) {
      LOGGER.info("Downloading extensions metadata from {}",
          WebClientFactory.obfuscateUri(repositoryUri));
      final URI extensionsApiUri = DocirUtil.getExtensionsApiUri(repositoryUri);
      ArrayList<DocirExtension> extensions = new ArrayList<>();
      for (DocirFlavorMetadata flavor : flavors) {
        final Extensions repositoryExtensions = client.getJson(
            UriBuilder.fromUri(extensionsApiUri)
            .queryParam("flavor", flavor.getFlavor().getName())
            .queryParam("major", flavor.getFlavor().getMajor())
            .queryParam("minor", flavor.getFlavor().getMinor())
            .build(), Extensions.class);
        extensions.addAll(Optional.ofNullable(repositoryExtensions.extensions())
            .map(foundExtensions -> DocirUtil.asDocirExtensions(
                repositoryUri.toASCIIString(),
                flavor.getFlavor().getName(),
                flavor.getFlavor().getVersion(),
                foundExtensions))
            .orElse(List.of()));
      }
      extensionsUriCache.put(repositoryUri, DocirExtensionMetadataCache.from(extensions));
    }
  }

  private void updateExtensionsCacheFromLocalResources(
      Collection<DocirFlavorMetadata> flavors, URI repositoryUri) throws Exception {
    if (!isCacheExpired(repositoryUri, Optional.ofNullable(extensionsUriCache.get(repositoryUri))
        .map(DocirExtensionMetadataCache::getCreated).orElse(null))) {
      return;
    }
    LOGGER.info("Reading extensions metadata from local resources");
    ArrayList<DocirExtension> extensions = new ArrayList<>();
    for (var flavor : flavors) {
      if (!hasResource(getExtensionsResourceUrl(flavor))) {
        continue;
      }
      try (var stream = getResourceAsStream(getExtensionsResourceUrl(flavor))) {
        var repositoryExtensions = getObjectMapper().readValue(stream, Extensions.class);
        extensions.addAll(Optional.ofNullable(repositoryExtensions.extensions())
            .map(foundExtensions -> DocirUtil.asDocirExtensions(
                repositoryUri.toASCIIString(),
                flavor.getFlavor().getName(),
                flavor.getFlavor().getVersion(),
                foundExtensions))
            .orElse(List.of()));
      }
    }
    extensionsUriCache.put(repositoryUri, DocirExtensionMetadataCache.from(extensions));
  }

  /**
   * The image of the patroni container of the cluster.
   */
  public String getImage(StackGresContext context, StackGresCluster cluster) {
    return getImageUrl(DocirImageIndex.fromCluster(context, cluster)).image().urlDigest();
  }

  /**
   * The image of the postgres-util container of the cluster: the image of the patroni container
   * under its own name.
   */
  public String getPostgresUtilImage(StackGresContext context, StackGresCluster cluster) {
    return getImageUrl(DocirImageIndex.fromClusterForPostgresUtil(context, cluster))
        .image().urlDigest();
  }

  /**
   * The partial image used to install the extension at runtime in the cluster.
   */
  public String getExtensionImage(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterInstalledExtension extension) {
    return getImageUrl(DocirImageIndex.fromExtension(context, cluster, extension))
        .image().urlDigest();
  }

  /**
   * The image of the patroni container used to perform the major version upgrade from
   * {@code oldCluster} to {@code cluster}.
   */
  public String getMajorUpgradeImage(
      StackGresContext context,
      StackGresCluster oldCluster,
      StackGresCluster cluster) {
    return getImageUrl(DocirImageIndex.fromClusters(context, cluster, oldCluster))
        .image().urlDigest();
  }

  /**
   * The image of a sidecar (or Job) container of the cluster that combines the base image and the
   * Postgres flavor pinned in the status of the cluster with a single addon.
   */
  public String getAddonImage(StackGresContext context, StackGresCluster cluster, String addon) {
    return getImageUrl(DocirImageIndex.fromAddon(context, cluster, addon)).image().urlDigest();
  }

  ImageUrl getImageUrl(DocirImageIndex index) {
    return getImagesMetadata(index).index.get(index);
  }

  synchronized DocirImageMetadataCache getImagesMetadata(DocirImageIndex index) {
    final boolean loadFromApi = onlyLoadFromApi
        || !hasResource(getImageUrlResourceUrl(index.getFlavor()));
    final URI repositoryUri;
    if (loadFromApi) {
      repositoryUri = URI.create(index.getFlavor().getFlavor().getRepository());
    } else {
      repositoryUri = LOCAL_RESOURCES_URI;
    }
    // The query describing every requested layer is also sent with the POST request: the REST API
    // ignores it while the cache of the Helm chart uses it to serve and store the response
    // without looking at the request body.
    final URI imageUri = URI.create(
        getImageCacheUriBuilder(repositoryUri, index).build()
        .toString().replace("%40", "@"));
    // The name of the image is carried in the fragment so that the same layers requested under
    // different names are cached apart while the query (the key of the local resources) is the
    // same.
    final URI imageCacheUri = URI.create(imageUri
        + Optional.ofNullable(index.getImageName()).map(name -> "#" + name).orElse(""));

    try {
      if (loadFromApi) {
        updateImageCacheFromApi(index, imageUri, imageCacheUri);
      } else {
        updateImageCacheFromLocalResources(index, imageUri, imageCacheUri);
      }
    } catch (Exception ex) {
      String message = "Can not retrieve image metadata from "
          + WebClientFactory.obfuscateUri(imageCacheUri);
      if (imageUriCache.get(imageCacheUri) != null) {
        LOGGER.warn(message, ex);
      } else {
        throw new RuntimeException(message, ex);
      }
    }

    return imageUriCache.get(imageCacheUri);
  }

  /**
   * The key under which the resolved image is cached (and looked up in the local resources): the
   * image URL API URI with a query describing every requested layer.
   */
  private UriBuilder getImageCacheUriBuilder(URI repositoryUri, DocirImageIndex index) {
    final UriBuilder imageUriBuilder =
        UriBuilder.fromUri(DocirUtil.getImageUrlApiUri(repositoryUri))
        .queryParam("tshirt-size", DocirUtil.DEFAULT_TSHIRT_SIZE);
    if (index.isPartial()) {
      imageUriBuilder
          .queryParam("flavorAndBaseOmmitted", true);
    } else {
      imageUriBuilder
          .queryParam("base", index.getFlavor().getBase().getName()
              + "@" + index.getFlavor().getBase().getVersion()
              + "@" + index.getFlavor().getBaseRevision());
    }
    imageUriBuilder
        .queryParam("flavors", index.getFlavor().getFlavor().getName()
            + "@" + index.getFlavor().getFlavor().getVersion()
            + "@" + index.getFlavor().getFlavorRevision().getRevision());
    if (!index.getExtensions().isEmpty()) {
      imageUriBuilder
          .queryParam("extensions", toExtensionsQueryValues(index.getExtensions()));
    }
    if (index.getOldFlavor() != null) {
      imageUriBuilder
          .queryParam("flavors", index.getOldFlavor().getFlavor().getName()
              + "@" + index.getOldFlavor().getFlavor().getVersion()
              + "@" + index.getOldFlavor().getFlavorRevision().getRevision());
      if (!index.getOldExtensions().isEmpty()) {
        imageUriBuilder
            .queryParam("extensions", toExtensionsQueryValues(index.getOldExtensions()));
      }
    }
    if (!index.getAddons().isEmpty()) {
      imageUriBuilder
          .queryParam("addons", index.getAddons().stream()
              .map(addon -> addon.getName()
                  + "@" + addon.getVersion()
                  + "@" + addon.getRevision())
              .toArray());
    }
    return imageUriBuilder;
  }

  private Object[] toExtensionsQueryValues(Collection<DocirExtensionMetadata> extensions) {
    return extensions.stream()
        .sorted(Comparator.comparing(Function.<DocirExtensionMetadata>identity()
            .andThen(DocirExtensionMetadata::getExtension)
            .andThen(DocirExtension::getName))
            .thenComparing(Comparator
                .comparing(Function.<DocirExtensionMetadata>identity()).reversed()))
        .map(extension -> extension.getExtension().getName()
            + "@" + extension.getVersion().getVersion()
            + "@" + extension.getRevision().getRevision())
        .toArray();
  }

  private void updateImageCacheFromApi(DocirImageIndex index, URI imageUri, URI imageCacheUri)
      throws Exception {
    if (!isCacheExpired(imageUri, Optional.ofNullable(imageUriCache.get(imageCacheUri))
        .map(DocirImageMetadataCache::getCreated).orElse(null))) {
      return;
    }
    try (WebClient client = createWebClient(imageUri)) {
      LOGGER.info("Getting image metadata from {}",
          WebClientFactory.obfuscateUri(imageUri));
      final ImageUrl imageUrl = client.postJson(imageUri, toImageUrlRequest(index), ImageUrl.class);
      if (imageUrl == null
          || imageUrl.image() == null
          || imageUrl.image().urlDigest() == null
          || imageUrl.image().urlDigest().isBlank()) {
        throw new IllegalStateException("The image returned by "
            + WebClientFactory.obfuscateUri(imageUri) + " for "
            + WebClientFactory.obfuscateUri(imageCacheUri) + " has no digest: "
            + Optional.ofNullable(imageUrl).map(ImageUrl::image).map(Object::toString)
            .orElse("null"));
      }
      imageUriCache.put(imageCacheUri, DocirImageMetadataCache.from(index, imageUrl));
    }
  }

  /**
   * Build the request of the image. Every layer is pinned to the revision (the monotonically
   * increasing id of its build) recorded in the status of the cluster and the greatest of them is
   * sent as the upper bound of the revision for any layer that has to be resolved by docir so that
   * the same request always resolves the same image.
   */
  ImageUrlRequest toImageUrlRequest(DocirImageIndex index) {
    final String revisionUpperBound = Seq
        .of(index.getFlavor().getBaseRevision(),
            index.getFlavor().getFlavorRevision().getRevision())
        .append(Seq.seq(index.getAddons())
            .map(StackGresClusterStatusAddon::getRevision))
        .append(Seq.seq(index.getExtensions())
            .map(extension -> extension.getRevision().getRevision()))
        .append(Optional.ofNullable(index.getOldFlavor())
            .map(oldFlavor -> oldFlavor.getFlavorRevision().getRevision())
            .stream())
        .append(Seq.seq(Optional.ofNullable(index.getOldExtensions()).orElse(Set.of()))
            .map(extension -> extension.getRevision().getRevision()))
        .filter(Objects::nonNull)
        .map(DocirRevision::new)
        .max(Comparator.naturalOrder())
        .map(DocirRevision::getRevision)
        .orElse(null);
    return new ImageUrlRequest(
        index.getImageName(),
        DocirUtil.DEFAULT_TSHIRT_SIZE,
        index.isPartial(),
        revisionUpperBound,
        new ImageUrlRequest.BaseRequest(
            index.getFlavor().getBase().getName(),
            String.valueOf(index.getFlavor().getBase().getMajor()),
            String.valueOf(index.getFlavor().getBase().getMinor()),
            index.getFlavor().getBaseRevision()),
        new ImageUrlRequest.FlavorRequest(
            index.getFlavor().getFlavor().getName(),
            Seq.of(toFlavorVersionRequest(index.getFlavor(), index.getExtensions()))
            .append(Optional.ofNullable(index.getOldFlavor())
                .map(oldFlavor -> toFlavorVersionRequest(oldFlavor, index.getOldExtensions())))
            .toArray(ImageUrlRequest.FlavorVersionRequest[]::new)),
        index.getAddons().stream()
        .map(addon -> new ImageUrlRequest.AddonRequest(
            addon.getName(),
            addon.getVersion(),
            addon.getRevision()))
        .toArray(ImageUrlRequest.AddonRequest[]::new));
  }

  private ImageUrlRequest.FlavorVersionRequest toFlavorVersionRequest(
      DocirFlavorMetadata flavor, Collection<DocirExtensionMetadata> extensions) {
    return new ImageUrlRequest.FlavorVersionRequest(
        String.valueOf(flavor.getFlavor().getMajor()),
        String.valueOf(flavor.getFlavor().getMinor()),
        flavor.getFlavor().getRevision(),
        extensions.stream()
        .map(extension -> new ImageUrlRequest.ExtensionRequest(
            extension.getExtension().getName(),
            extension.getVersion().getVersion(),
            extension.getRevision().getRevision()))
        .toArray(ImageUrlRequest.ExtensionRequest[]::new));
  }

  private void updateImageCacheFromLocalResources(
      DocirImageIndex index, URI imageUri, URI imageCacheUri) throws Exception {
    if (!isCacheExpired(imageUri, Optional.ofNullable(imageUriCache.get(imageCacheUri))
        .map(DocirImageMetadataCache::getCreated).orElse(null))) {
      return;
    }
    LOGGER.info("Reading image metadata from local resources");
    try (var stream = getResourceAsStream(getImageUrlResourceUrl(index.getFlavor()))) {
      var docirImageMap = getObjectMapper().readValue(
          stream,
          ImageUrlMapTypeReference.INSTANCE);
      if (!docirImageMap.containsKey(imageCacheUri.getQuery())) {
        throw new RuntimeException("Image for query " + imageCacheUri.getQuery()
            + " was not found in resource " + getImageUrlResourceUrl(index.getFlavor()));
      }
      imageUriCache.put(imageCacheUri, DocirImageMetadataCache.from(
          index, docirImageMap.get(imageCacheUri.getQuery())));
    }
  }

  private String getBaseImagesResourceUrl() {
    return "base-images.json";
  }

  private String getVersionsResourceUrl(String flavor) {
    return "versions-" + flavor + ".json";
  }

  private String getAddonsResourceUrl() {
    return "addons.json";
  }

  private String getExtensionsResourceUrl(DocirFlavorMetadata flavorMetadata) {
    return "extensions-" + flavorMetadata.getFlavor().getName()
        + "-" + flavorMetadata.getFlavor().getMajor() + "." + flavorMetadata.getFlavor().getMinor()
        + ".json";
  }

  private String getImageUrlResourceUrl(DocirFlavorMetadata flavorMetadata) {
    return "image-urls-" + flavorMetadata.getFlavor().getName()
        + "-" + flavorMetadata.getFlavor().getMajor() + "." + flavorMetadata.getFlavor().getMinor()
        + ".json";
  }

  private boolean hasResource(String resourceName) {
    return ClassLoader.getSystemClassLoader().getResource(resourceName) != null;
  }

  private InputStream getResourceAsStream(String resourceName) {
    return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
  }

  static final class ImageUrlMapTypeReference extends TypeReference<Map<String, ImageUrl>> {
    static final ImageUrlMapTypeReference INSTANCE = new ImageUrlMapTypeReference();
  }

  static class DocirFlavorMetadataCache {
    final Instant created;
    final Map<DocirFlavorIndex, DocirFlavorMetadata> index;
    final List<DocirAddon> addons;

    DocirFlavorMetadataCache(
        Map<DocirFlavorIndex, DocirFlavorMetadata> index,
        List<DocirAddon> addons) {
      this.created = Instant.now();
      this.index = index;
      this.addons = addons;
    }

    static DocirFlavorMetadataCache from(
        List<DocirFlavor> flavors,
        List<DocirBase> bases,
        List<DocirAddon> addons) {
      return new DocirFlavorMetadataCache(
          DocirUtil.toFlavorsMetadataIndex(flavors, bases, addons),
          addons);
    }

    public Instant getCreated() {
      return created;
    }
  }

  static class DocirExtensionMetadataCache {
    final Instant created;
    final Map<DocirExtensionIndex, DocirExtensionMetadata> index;
    final Map<DocirExtensionIndexSameMajorRevision, List<DocirExtensionMetadata>>
        indexSameMajorBuilds;
    final Map<DocirExtensionIndexAnyVersion, List<DocirExtensionMetadata>>
        indexAnyVersions;

    DocirExtensionMetadataCache(
        Map<DocirExtensionIndex, DocirExtensionMetadata> index,
        Map<DocirExtensionIndexSameMajorRevision, List<DocirExtensionMetadata>>
            indexSameMajorBuilds,
        Map<DocirExtensionIndexAnyVersion, List<DocirExtensionMetadata>>
            indexAnyVersions) {
      this.created = Instant.now();
      this.index = index;
      this.indexSameMajorBuilds = indexSameMajorBuilds;
      this.indexAnyVersions = indexAnyVersions;
    }

    static DocirExtensionMetadataCache from(List<DocirExtension> extensions) {
      return new DocirExtensionMetadataCache(
          DocirUtil.toExtensionsMetadataIndex(extensions),
          DocirUtil.toExtensionsMetadataIndexSameMajorRevisions(extensions),
          DocirUtil.toExtensionsMetadataIndexAnyVersions(extensions));
    }

    public Instant getCreated() {
      return created;
    }
  }

  static class DocirImageMetadataCache {
    final Instant created;
    final Map<DocirImageIndex, ImageUrl> index;

    DocirImageMetadataCache(
        Map<DocirImageIndex, ImageUrl> index) {
      this.created = Instant.now();
      this.index = index;
    }

    static DocirImageMetadataCache from(DocirImageIndex index, ImageUrl image) {
      return new DocirImageMetadataCache(Map.of(index, image));
    }

    public Instant getCreated() {
      return created;
    }
  }

}
