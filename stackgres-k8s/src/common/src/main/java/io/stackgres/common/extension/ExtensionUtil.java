/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static io.stackgres.common.OsDetector.OS_DETECTOR;
import static io.stackgres.common.OsDetector.getClusterArch;
import static io.stackgres.common.OsDetector.getClusterOs;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.OsDetector;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import jakarta.ws.rs.core.UriBuilder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple4;

public interface ExtensionUtil {

  String DEFAULT_CHANNEL = "stable";
  String DEFAULT_PUBLISHER = "com.ongres";
  String DEFAULT_FLAVOR = "pg";
  String DEFAULT_ARCH = OsDetector.ARCH_X86_64;
  String DEFAULT_OS = OsDetector.OS_LINUX;

  static Map<StackGresExtensionIndex, StackGresExtensionMetadata> toExtensionsMetadataIndex(
      URI repositoryBaseUri, StackGresExtensions currentExtensionsMetadata) {
    Preconditions.checkArgument(Objects.isNull(repositoryBaseUri.getRawQuery()));
    return currentExtensionsMetadata.getExtensions().stream()
        .peek(extension -> {
          if (extension.getRepository() == null) {
            extension.setRepository(repositoryBaseUri.toString());
          }
        })
        .flatMap(extension -> extension.getVersions().stream()
            .map(version -> Tuple.tuple(extension, version)))
        .flatMap(t -> t.v2.getAvailableFor().stream().map(t::concat))
        .collect(Collectors.toMap(
            t -> new StackGresExtensionIndex(t.v1, t.v2, t.v3),
            t -> new StackGresExtensionMetadata(t.v1, t.v2, t.v3,
                currentExtensionsMetadata.getPublishers().stream()
                .filter(publisher -> publisher.getId().equals(t.v1.getPublisherOrDefault()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Can not find available version of extension "
                    + getDescription(t.v1, t.v2, t.v3))))));
  }

  static Map<StackGresExtensionIndexSameMajorBuild, List<StackGresExtensionMetadata>>
      toExtensionsMetadataIndexSameMajorBuilds(URI repositoryBaseUri,
          StackGresExtensions currentExtensionsMetadata) {
    Preconditions.checkArgument(Objects.isNull(repositoryBaseUri.getRawQuery()));
    return Seq.seq(currentExtensionsMetadata.getExtensions())
        .peek(extension -> {
          if (extension.getRepository() == null) {
            extension.setRepository(repositoryBaseUri.toString());
          }
        })
        .flatMap(extension -> extension.getVersions().stream()
            .map(version -> Tuple.tuple(extension, version)))
        .flatMap(t -> t.v2.getAvailableFor().stream().map(t::concat))
        .map(t -> t.concat(new StackGresExtensionIndexSameMajorBuild(t.v1, t.v2, t.v3)))
        .grouped(Tuple4::v4)
        .collect(Collectors.toMap(
            t -> t.v1,
            t -> t.v2.map(tt -> new StackGresExtensionMetadata(tt.v1, tt.v2, tt.v3,
                currentExtensionsMetadata.getPublishers().stream()
                .filter(publisher -> publisher.getId().equals(tt.v1.getPublisherOrDefault()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Can not find available version of extension "
                    + getDescription(tt.v1, tt.v2, tt.v3)))))
            .toList()));
  }

  static Map<StackGresExtensionIndexAnyVersion, List<StackGresExtensionMetadata>>
      toExtensionsMetadataIndexAnyVersions(URI repositoryBaseUri,
          StackGresExtensions currentExtensionsMetadata) {
    Preconditions.checkArgument(Objects.isNull(repositoryBaseUri.getRawQuery()));
    return Seq.seq(currentExtensionsMetadata.getExtensions())
        .peek(extension -> {
          if (extension.getRepository() == null) {
            extension.setRepository(repositoryBaseUri.toString());
          }
        })
        .flatMap(extension -> extension.getVersions().stream()
            .map(version -> Tuple.tuple(extension, version)))
        .flatMap(t -> t.v2.getAvailableFor().stream().map(t::concat))
        .map(t -> t.concat(new StackGresExtensionIndexAnyVersion(t.v1, t.v3)))
        .grouped(Tuple4::v4)
        .toMap(
            t -> t.v1,
            t -> t.v2.map(tt -> new StackGresExtensionMetadata(tt.v1, tt.v2, tt.v3,
                currentExtensionsMetadata.getPublishers().stream()
                .filter(publisher -> publisher.getId().equals(tt.v1.getPublisherOrDefault()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Can not find available version of extension "
                    + getDescription(tt.v1, tt.v2, tt.v3)))))
            .toList());
  }

  static Map<String, StackGresExtensionPublisher> toPublishersIndex(
      StackGresExtensions extensions) {
    return Seq.seq(extensions.getPublishers())
        .collect(ImmutableMap.toImmutableMap(
            StackGresExtensionPublisher::getId, Function.identity()));
  }

  static String getExtensionPackageName(
      StackGresCluster cluster,
      StackGresClusterInstalledExtension extension) {

    return getExtensionPackageName(
        getPostgresFlavorComponent(cluster),
        extension
    );
  }

  static String getExtensionPackageName(
      StackGresComponent component,
      StackGresClusterInstalledExtension extension) {
    final Optional<String> buildVersion = Optional.ofNullable(
        extension.getBuild());
    return extension.getName()
        + "-" + extension.getVersion()
        + "-" + getFlavorPrefix(component)
        + extension.getPostgresVersion()
        + buildVersion.map(build -> "-build-" + build).orElse("");
  }

  static URI getExtensionPackageUri(URI repositoryUri,
      StackGresCluster cluster,
      StackGresClusterInstalledExtension installedExtension) {
    return UriBuilder.fromUri(repositoryUri)
        .path(installedExtension.getPublisher())
        .path(getClusterArch(cluster)).path(getClusterOs(cluster))
        .path(getExtensionPackageName(cluster, installedExtension) + ".tar")
        .build();
  }

  static StackGresClusterInstalledExtension getInstalledExtension(StackGresCluster cluster,
      StackGresClusterExtension extension, StackGresExtensionMetadata extensionMetadata,
      boolean detectOs) {
    StackGresClusterInstalledExtension installedExtension =
        new StackGresClusterInstalledExtension();

    installedExtension.setName(extensionMetadata.getExtension().getName());
    installedExtension.setPublisher(extensionMetadata.getExtension().getPublisherOrDefault());
    installedExtension.setVersion(extensionMetadata.getVersion().getVersion());
    installedExtension.setExtraMounts(extensionMetadata.getVersion().getExtraMounts());
    installedExtension.setRepository(getExtensionRepositoryUri(extension, extensionMetadata)
        .orElseThrow(() -> new RuntimeException("URI not found for extension "
            + ExtensionUtil.getDescription(extensionMetadata)))
        .toString());
    installedExtension.setPostgresVersion(extensionMetadata.getTarget().getPostgresVersion());
    if (StackGresUtil.isRegistryEnabled(cluster)
        || getClusterArch(cluster, Optional.of(OS_DETECTOR).filter(od -> detectOs)).isPresent()) {
      installedExtension.setBuild(extensionMetadata.getTarget().getBuild());
    }

    return installedExtension;
  }

  static Optional<URI> getExtensionRepositoryUri(StackGresClusterExtension extension,
      StackGresExtensionMetadata extensionMetadata) {
    return Optional.ofNullable(extension.getRepository())
        .or(() -> Optional.ofNullable(extensionMetadata.getExtension().getRepository()))
        .map(URI::create);
  }

  static URI getIndexUri(URI extensionsUrl) {
    return UriBuilder.fromUri(extensionsUrl).path("/v2/index.json").build();
  }

  static String getDescription(StackGresCluster cluster,
      StackGresClusterExtension extension, boolean detectOs) {
    final String pgMajorVersion = getPostgresFlavorComponent(cluster).get(cluster)
        .getMajorVersion(null, cluster.getSpec().getPostgres().getVersion());
    final Optional<OsDetector> osDetector = Optional.of(OS_DETECTOR).filter(od -> detectOs);
    return extension.getPublisherOrDefault() + "/" + extension.getName()
        + " for version " + extension.getVersionOrDefaultChannel()
        + "[" + getFlavorPrefix(cluster) + pgMajorVersion
        + osDetector.map(od -> "/" + od.getArch() + "/" + od.getOs()).orElse("") + "]";
  }

  static String getDescription(StackGresCluster cluster,
      StackGresClusterInstalledExtension extension, boolean detectOs) {
    final Optional<OsDetector> osDetector = Optional.of(OS_DETECTOR).filter(od -> detectOs);
    return extension.getPublisher() + "/" + extension.getName()
        + " for version " + extension.getVersion()
        + "[" + getFlavorPrefix(cluster) + extension.getPostgresVersion()
        + osDetector.map(od -> "/" + od.getArch() + "/" + od.getOs()).orElse("") + "]";
  }

  static String getDescription(StackGresExtensionMetadata extensionMetadata) {
    return getDescription(extensionMetadata.getExtension(),
        extensionMetadata.getVersion(), extensionMetadata.getTarget());
  }

  static String getDescription(StackGresExtension extension,
      StackGresExtensionVersion version, StackGresExtensionVersionTarget target) {
    return extension.getPublisherOrDefault() + "/" + extension.getName()
        + " for version " + version.getVersion()
        + "[" + target.getFlavorOrDefault() + target.getPostgresVersion()
        + "/" + target.getArchOrDefault()
        + "/" + target.getOsOrDefault() + "]";
  }

  static String getFlavorPrefix(StackGresCluster cluster) {
    var component = getPostgresFlavorComponent(cluster);
    return getFlavorPrefix(component);
  }

  static String getFlavorPrefix(StackGresComponent component) {
    switch (component) {
      case POSTGRESQL:
        return "pg";
      case BABELFISH:
        return "bf";
      default:
        throw new IllegalArgumentException("Component " + component + " is not a valid flavor");
    }
  }

  static String getMajorBuildOrNull(String build) {
    if (build == null) {
      return null;
    }
    return build.split(Pattern.quote("."))[0];
  }

}
