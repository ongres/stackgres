/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import static io.stackgres.common.OsDetector.OS_DETECTOR;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.stackgres.common.OsDetector;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.Component;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;
import io.stackgres.common.docir.model.Addon;
import io.stackgres.common.docir.model.BaseImage;
import io.stackgres.common.docir.model.Extension;
import io.stackgres.common.docir.model.PlatformWithRevision;
import io.stackgres.common.docir.model.Version;
import jakarta.ws.rs.core.UriBuilder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

public interface DocirUtil {

  /**
   * Base path of the StackGres docir REST API relative to the repository URL (see
   * {@code SGConfig.spec.repository.url}).
   */
  String API_PATH = "/api/stackgres/v1";
  /**
   * Query parameter of the catalog endpoints with the {@code <major>.<minor>} version of the
   * operator that the served images must support.
   */
  String OPERATOR_VERSION_PARAMETER = "operator-version";
  /**
   * Query parameter of the catalog endpoints that restricts the served images to the published
   * ones (when {@code true}).
   */
  String PUBLISHED_PARAMETER = "published";
  String POSTGRES_FLAVOR = "postgres";
  String BABELFISH_FLAVOR = "babelfishpg";
  List<String> SUPPORTED_FLAVORS = List.of(POSTGRES_FLAVOR, BABELFISH_FLAVOR);

  record DocirPlatform(String os, String arch) { }

  List<DocirPlatform> SUPPORTED_PLATFORMS = OsDetector.SUPPORTED_OSES
          .stream()
          .flatMap(os -> OsDetector.SUPPORTED_ARCHS
              .stream()
              .map(DocirUtil::toDocirArch)
              .map(arch -> new DocirPlatform(os, arch)))
      .toList();

  String DEFAULT_TSHIRT_SIZE = "full";
  String PATRONI_ADDON = "patroni";
  String WALG_ADDON = "wal-g";
  String HDRHISTOGRAM_ADDON = "hdrhistogram";
  String PGBOUNCER_ADDON = "pgbouncer";
  String POSTGRES_EXPORTER_ADDON = "postgres-exporter";
  String KUBECTL_ADDON = "kubectl";
  String FLUENT_BIT_ADDON = "fluent-bit";
  String FLUENTD_ADDON = "fluentd";
  String OTEL_COLLECTOR_ADDON = "otel-collector";
  /**
   * Prefix of the name of the image of the postgres-util container, that reuses the layers of the
   * image of the patroni container.
   */
  String POSTGRES_UTIL_IMAGE = "postgres-util";
  /**
   * The addons combined with the base image, the Postgres flavor and the extensions in the image
   * of the patroni container (also used by the SGDbOps Jobs, hence hdrhistogram for pgbench).
   */
  List<String> POSTGRES_IMAGE_ADDONS = List.of(PATRONI_ADDON, WALG_ADDON, HDRHISTOGRAM_ADDON);
  /**
   * The addons that are each combined alone with the base image and the Postgres flavor to build
   * the image of a sidecar (or Job) container.
   */
  Map<StackGresComponent, String> SIDECAR_ADDONS = Map.of(
      StackGresComponent.PGBOUNCER, PGBOUNCER_ADDON,
      StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER, POSTGRES_EXPORTER_ADDON,
      StackGresComponent.KUBECTL, KUBECTL_ADDON,
      StackGresComponent.FLUENT_BIT, FLUENT_BIT_ADDON,
      StackGresComponent.FLUENTD, FLUENTD_ADDON,
      StackGresComponent.OTEL_COLLECTOR, OTEL_COLLECTOR_ADDON);
  /**
   * The docir addon of every component whose image or version is served by docir.
   */
  Map<StackGresComponent, String> COMPONENT_ADDONS = Stream.concat(
      Stream.of(
          Map.entry(StackGresComponent.PATRONI, PATRONI_ADDON),
          Map.entry(StackGresComponent.WALG, WALG_ADDON),
          Map.entry(StackGresComponent.HDRHISTOGRAM, HDRHISTOGRAM_ADDON)),
      SIDECAR_ADDONS.entrySet().stream())
      .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  String DEFAULT_PUBLISHER = "docir";
  String DEFAULT_FLAVOR = "pg";
  String DEFAULT_OS = OsDetector.OS_LINUX;
  String DEFAULT_ARCH = OsDetector.ARCH_X86_64;

  static String getAddonName(StackGresComponent component) {
    return Optional.ofNullable(COMPONENT_ADDONS.get(component))
        .orElseThrow(() -> new IllegalArgumentException(
            "Component " + component + " has no docir addon"));
  }

  /**
   * The addons pinned in the status of the cluster among the ones requested.
   */
  static List<StackGresClusterStatusAddon> getStatusAddons(
      StackGresCluster cluster, List<String> addons) {
    return addons.stream()
        .map(addon -> cluster.getStatus().findAddon(addon))
        .flatMap(Optional::stream)
        .toList();
  }

  static boolean hasStatusAddons(StackGresCluster cluster, List<String> addons) {
    return cluster.getStatus() != null
        && addons.stream().allMatch(addon -> cluster.getStatus().findAddon(addon).isPresent());
  }

  /**
   * The query parameters that filter the catalog served by the docir REST API (base images,
   * versions, addons and extensions): {@code operator-version} (the {@code <major>.<minor>} version
   * of the operator) restricts the catalog to the images that support this operator version and
   * {@code published=true} to the images that have been published, unless
   * {@link DocirConfigUtil#isUsePublishedImages()} is disabled in which case unpublished images
   * are also served.
   */
  static UriBuilder getCatalogApiUriBuilder(URI docirRepositoryUri, String path) {
    final UriBuilder uriBuilder = UriBuilder.fromUri(docirRepositoryUri)
        .path(API_PATH + path)
        .queryParam(OPERATOR_VERSION_PARAMETER, getOperatorVersion());
    if (DocirConfigUtil.isUsePublishedImages()) {
      uriBuilder.queryParam(PUBLISHED_PARAMETER, true);
    }
    return uriBuilder;
  }

  /**
   * The {@code <major>.<minor>} version of the operator sent to the docir REST API.
   */
  static String getOperatorVersion() {
    return StackGresVersion.LATEST.getVersion();
  }

  static URI getBaseImagesApiUri(URI docirRepositoryUri) {
    return getCatalogApiUriBuilder(docirRepositoryUri, "/base-images").build();
  }

  static URI getVersionsApiUri(URI docirRepositoryUri) {
    return getCatalogApiUriBuilder(docirRepositoryUri, "/versions").build();
  }

  static URI getAddonsApiUri(URI docirRepositoryUri) {
    return getCatalogApiUriBuilder(docirRepositoryUri, "/addons").build();
  }

  static URI getExtensionsApiUri(URI docirRepositoryUri) {
    return getCatalogApiUriBuilder(docirRepositoryUri, "/extensions").build();
  }

  static URI getImageUrlApiUri(URI docirRepositoryUri) {
    return UriBuilder.fromUri(docirRepositoryUri).path(API_PATH + "/image-url").build();
  }

  static String getFlavorName(StackGresCluster cluster) {
    var component = getPostgresFlavorComponent(cluster);
    return getFlavorName(component);
  }

  static String getFlavorName(StackGresComponent component) {
    switch (component) {
      case POSTGRESQL:
        return "postgres";
      case BABELFISH:
        return "babelfishpg";
      default:
        throw new IllegalArgumentException("Component " + component + " is not a valid flavor");
    }
  }

  static String getFlavorName(Component component) {
    switch (component.getName()) {
      case "postgresql":
        return "postgres";
      case "babelfish":
        return "babelfishpg";
      default:
        throw new IllegalArgumentException("Component " + component.getName() + " is not a valid flavor");
    }
  }

  static StackGresComponent getComponent(DocirFlavorMetadata flavorMetadata) {
    switch (flavorMetadata.getFlavor().getName()) {
      case "postgres":
        return StackGresComponent.POSTGRESQL;
      case "babelfishpg":
        return StackGresComponent.BABELFISH;
      default:
        throw new IllegalArgumentException(
            "Flavor " + flavorMetadata.getFlavor().getName() + " is not a valid flavor");
    }
  }

  static String getDescription(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      boolean detectOs) {
    final String pgMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(context, cluster.getSpec().getPostgres().getVersion());
    final Optional<OsDetector> osDetector = Optional.of(OS_DETECTOR).filter(od -> detectOs);
    return extension.getPublisherOrDefault() + "/" + extension.getName()
        + " for version " + extension.getVersionOrDefaultChannel()
        + "[" + getFlavorName(cluster) + pgMajorVersion
        + osDetector.map(od -> "/" + od.getArch() + "/" + od.getOs()).orElse("") + "]";
  }

  static String getDescription(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension extension,
      boolean detectOs) {
    final Optional<OsDetector> osDetector = Optional.of(OS_DETECTOR).filter(od -> detectOs);
    return Optional.ofNullable(extension.getPublisher()).orElse(DEFAULT_PUBLISHER) + "/" + extension.getName()
        + " for version " + extension.getVersion()
        + "[" + getFlavorName(cluster) + extension.getPostgresVersion()
        + osDetector.map(od -> "/" + od.getArch() + "/" + od.getOs()).orElse("") + "]";
  }

  static String getDescription(DocirExtensionMetadata extensionMetadata) {
    return getDescription(extensionMetadata.getExtension(),
        extensionMetadata.getVersion());
  }

  static String getDescription(DocirExtension extension,
      DocirExtensionVersion version) {
    return extension.getRepository() + "/" + extension.getName()
        + " for version " + version.getVersion()
        + "[" + version.getFlavorOrDefault() + version.getFlavorVersion()
        + "/" + version.getArchOrDefault()
        + "/" + version.getOsOrDefault() + "]";
  }

  static List<DocirBase> asDocirBases(
      String repository,
      BaseImage[] baseImagesMetadata) {
    return Seq.of(baseImagesMetadata)
        .flatMap(baseImage -> Stream.of(baseImage.platforms())
            .filter(DocirUtil::isSupportedPlatform)
            .map(platform -> DocirBase.fromMetadata(
                repository, baseImage, platform)))
        .toList();
  }

  static List<DocirFlavor> asDocirFlavors(
      String repository,
      String flavor,
      Version[] versionMetadata) {
    return Seq.of(versionMetadata)
        .filter(version -> !version.isLatest()
            && !version.isLatestMinor())
        .flatMap(version -> Stream.of(version.platforms())
            .filter(DocirUtil::isSupportedPlatform)
            .map(platform -> DocirFlavor.fromMetadata(
                repository, flavor, version, platform)))
        .toList();
  }

  static List<DocirAddon> asDocirAddons(
      String repository,
      Addon[] addonMetadata) {
    return Seq.of(addonMetadata)
        .map(addon -> DocirAddon.fromMetadata(
            repository,
            addon,
            Stream.of(addon.versions())
            .flatMap(version -> Stream.of(version.platforms())
                .filter(DocirUtil::isSupportedPlatform)
                .map(platform -> DocirAddonVersion.fromMetadata(
                    version, platform)))
            .toList()))
        .toList();
  }

  static List<DocirExtension> asDocirExtensions(
      String repository,
      String flavor,
      String flavorVersion,
      Extension[] extensionsMetadata) {
    return Seq.of(extensionsMetadata)
        .map(extension -> DocirExtension.fromMetadata(
            repository,
            extension,
            Stream.of(extension.extensionVersions())
            .flatMap(version -> Stream.of(version.platforms())
                .filter(DocirUtil::isSupportedPlatform)
                .map(platform -> DocirExtensionVersion.fromMetadata(
                    flavor, flavorVersion, version, platform)))
            .toList()))
        .toList();
  }

  static boolean isSupportedPlatform(PlatformWithRevision platform) {
    return SUPPORTED_PLATFORMS.stream()
        .anyMatch(supported -> supported.os.equals(platform.os())
            && supported.arch.equals(platform.architecture())
            && platform.variant() == null);
  }

  static Map<DocirExtensionIndex, DocirExtensionMetadata> toExtensionsMetadataIndex(
      List<DocirExtension> currentExtensionsMetadata) {
    return Seq.seq(currentExtensionsMetadata)
        .flatMap(extension -> extension.getVersions().stream()
            .map(version -> Tuple.tuple(extension, version)))
        .map(t -> Tuple.tuple(
            new DocirExtensionIndex(t.v1, t.v2),
            new DocirExtensionMetadata(t.v1, t.v2)))
        .grouped(Tuple2::v1)
        .flatMap(t -> t.v2.limit(1))
        .collect(Collectors.toMap(
            Tuple2::v1,
            Tuple2::v2));
  }

  static Map<DocirExtensionIndexSameMajorRevision, List<DocirExtensionMetadata>>
      toExtensionsMetadataIndexSameMajorRevisions(
          List<DocirExtension> currentExtensionsMetadata) {
    return Seq.seq(currentExtensionsMetadata)
        .flatMap(extension -> extension.getVersions().stream()
            .map(version -> Tuple.tuple(extension, version)))
        .map(t -> t.concat(new DocirExtensionIndexSameMajorRevision(t.v1, t.v2)))
        .grouped(Tuple3::v3)
        .collect(Collectors.toMap(
            t -> t.v1,
            t -> t.v2
            .map(tt -> new DocirExtensionMetadata(tt.v1, tt.v2))
            .grouped(Function.<DocirExtensionMetadata>identity()
                .andThen(DocirExtensionMetadata::getVersion)
                .andThen(DocirExtensionVersion::getVersion))
            .flatMap(tt -> tt.v2
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .stream())
            .sorted(Comparator.reverseOrder())
            .toList()));
  }

  static Map<DocirExtensionIndexAnyVersion, List<DocirExtensionMetadata>>
      toExtensionsMetadataIndexAnyVersions(
          List<DocirExtension> currentExtensionsMetadata) {
    return Seq.seq(currentExtensionsMetadata)
        .flatMap(extension -> extension.getVersions().stream()
            .map(version -> Tuple.tuple(extension, version)))
        .map(t -> t.concat(new DocirExtensionIndexAnyVersion(t.v1, t.v2)))
        .grouped(Tuple3::v3)
        .toMap(
            t -> t.v1,
            t -> t.v2
            .map(tt -> new DocirExtensionMetadata(tt.v1, tt.v2))
            .grouped(Function.<DocirExtensionMetadata>identity()
                .andThen(DocirExtensionMetadata::getVersion)
                .andThen(DocirExtensionVersion::getVersion))
            .flatMap(tt -> tt.v2
                .sorted(Comparator.reverseOrder())
                .findFirst()
                .stream())
            .sorted(Comparator.reverseOrder())
            .toList());
  }

  static Map<DocirFlavorIndex, DocirFlavorMetadata> toFlavorsMetadataIndex(
      List<DocirFlavor> currentFlavors,
      List<DocirBase> currentBases,
      List<DocirAddon> currentAddons) {
    return Seq.seq(currentFlavors)
        .filter(flavor -> flavor.getMajor() != null && flavor.getMinor() != null)
        .flatMap(flavorRevision -> currentBases.stream()
            .filter(base -> isRequiredBase(flavorRevision, base))
            .map(base -> Tuple.tuple(flavorRevision, base)))
        .flatMap(flavorRevision -> currentAddons.stream()
            .filter(addon -> isRequiredAddon(PATRONI_ADDON, flavorRevision.v1, addon))
            .flatMap(patroni -> patroni.getVersions().stream()
                .filter(version -> isCompatibleAddonVersion(flavorRevision.v1, version))
                .map(flavorRevision::concat)))
        .flatMap(flavorRevision -> currentAddons.stream()
            .filter(addon -> isRequiredAddon(WALG_ADDON, flavorRevision.v1, addon))
            .flatMap(walg -> walg.getVersions().stream()
                .filter(version -> isCompatibleAddonVersion(flavorRevision.v1, version))
                .map(flavorRevision::concat)))
        .flatMap(flavorRevision -> currentAddons.stream()
            .filter(addon -> isRequiredAddon(HDRHISTOGRAM_ADDON, flavorRevision.v1, addon))
            .flatMap(hdrhistogram -> hdrhistogram.getVersions().stream()
                .filter(version -> isCompatibleAddonVersion(flavorRevision.v1, version))
                .map(flavorRevision::concat)))
        .map(t -> Tuple.tuple(
            new DocirFlavorIndex(t.v1, t.v2, t.v3, t.v4, t.v5),
            new DocirFlavorMetadata(t.v1, t.v2, t.v3, t.v4, t.v5)))
        .grouped(Tuple2::v1)
        .flatMap(t -> t.v2.limit(1))
        .collect(Collectors.toMap(
            Tuple2::v1,
            Tuple2::v2));
  }

  static boolean isRequiredBase(DocirFlavor flavor, DocirBase base) {
    return Objects.equals(base.getName(), flavor.getBaseName())
        && Objects.equals(base.getMajor(), flavor.getBaseMajor())
        && Objects.equals(base.getMinor(), flavor.getBaseMinor())
        && Objects.equals(base.getOsOrDefault(), flavor.getOsOrDefault())
        && Objects.equals(base.getArchOrDefault(), flavor.getArchOrDefault());
  }

  static boolean isRequiredAddon(String name, DocirFlavor flavor, DocirAddon addon) {
    return Objects.equals(addon.getName(), name)
        && addon.getVersions().stream()
        .anyMatch(version -> isCompatibleAddonVersion(flavor, version));
  }

  /**
   * Addons are independent from the flavor: an addon is compatible with a flavor when both were
   * built on the same base image (name, major and minor version) for the same platform.
   */
  static boolean isCompatibleAddonVersion(DocirFlavor flavor, DocirAddonVersion version) {
    return Objects.equals(version.getBaseIdentity(), flavor.getBaseIdentity())
        && Objects.equals(version.getOsOrDefault(), flavor.getOsOrDefault())
        && Objects.equals(version.getArchOrDefault(), flavor.getArchOrDefault());
  }

  static boolean isCompatibleAddonVersion(
      DocirBase base, String os, String arch, DocirAddonVersion version) {
    return Objects.equals(version.getBaseIdentity(), base.getBaseIdentity())
        && Objects.equals(version.getOsOrDefault(), os)
        && Objects.equals(version.getArchOrDefault(), arch);
  }

  /**
   * The latest version (and, among builds of the same version, the latest revision) of the addon
   * built on the base image for the platform.
   */
  static Optional<DocirAddonVersion> findLatestAddonVersion(
      List<DocirAddon> addons, String name, DocirBase base, String os, String arch) {
    return addons.stream()
        .filter(addon -> Objects.equals(addon.getName(), name))
        .flatMap(addon -> addon.getVersions().stream())
        .filter(version -> isCompatibleAddonVersion(base, os, arch, version))
        .max(Comparator
            .comparing((DocirAddonVersion version) -> StackGresUtil.sortableVersion(version.getVersion()))
            .thenComparing(version -> new DocirRevision(version.getRevision())));
  }

  record PostgresMajorMinor(Integer major, Integer minor) {}

  static PostgresMajorMinor getPostgresMajorMinor(String version) {
    int pointIndex = version.indexOf('.');
    return new PostgresMajorMinor(
        Integer.valueOf(version.substring(0, pointIndex)),
        Integer.valueOf(version.substring(pointIndex + 1)));
  }

  record BaseMajorMinor(Integer major, Integer minor) {}

  static BaseMajorMinor getBaseMajorMinor(String version) {
    int pointIndex = version.indexOf('.');
    return new BaseMajorMinor(
        Integer.valueOf(version.substring(0, pointIndex)),
        Integer.valueOf(version.substring(pointIndex + 1)));
  }

  static String toDocirArch(final String arch) {
    final String archLowerCase = arch.toLowerCase(Locale.US);
    switch (archLowerCase) {
      case OsDetector.ARCH_X86_64:
        return "amd64";
      case OsDetector.ARCH_AARCH64:
        return "arm64";
      default:
        return archLowerCase;
    }
  }

  static String getBaseIdentity(String baseName, Integer baseMajor, Integer baseMinor) {
    if (baseName == null || baseMajor == null || baseMinor == null) {
      return null;
    }
    return baseName + "-" + baseMajor + "." + baseMinor;
  }

  static Integer parseIntegerOrNull(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  static StackGresClusterInstalledExtension getInstalledExtension(
      StackGresCluster cluster,
      StackGresClusterExtension extension,
      DocirExtensionMetadata extensionMetadata,
      boolean detectOs) {
    StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();

    installedExtension.setName(extensionMetadata.getExtension().getName());
    installedExtension.setVersion(extensionMetadata.getVersion().getVersion());
    installedExtension.setRepository(getDocirRepositoryUri(extension, extensionMetadata)
        .orElseThrow(() -> new RuntimeException("URI not found for extension "
            + DocirUtil.getDescription(extensionMetadata)))
        .toString());
    installedExtension.setPostgresVersion(extensionMetadata.getVersion().getFlavorVersion());
    installedExtension.setBuild(extensionMetadata.getRevision().getRevision());

    return installedExtension;
  }

  static Optional<URI> getDocirRepositoryUri(
      StackGresClusterExtension extension,
      DocirExtensionMetadata extensionMetadata) {
    return Optional.ofNullable(extension.getRepository())
        .or(() -> Optional.ofNullable(extensionMetadata.getExtension().getRepository()))
        .map(URI::create);
  }

  static String getExtensionFileName(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension extension) {
    return extension.getName() + "-" + extension.getVersion()
        + "-" + extension.getPostgresVersion() + "-revision-" + extension.getBuild();
  }

}
