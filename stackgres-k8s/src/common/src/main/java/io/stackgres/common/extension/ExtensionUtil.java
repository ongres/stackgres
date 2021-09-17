/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;

public interface ExtensionUtil {

  String DEFAULT_CHANNEL = "stable";
  String DEFAULT_PUBLISHER = "com.ongres";
  String ARCH_X86_64 = "x86_64";
  String DEFAULT_ARCH = ARCH_X86_64;
  String OS_LINUX = "linux";
  String DEFAULT_OS = OS_LINUX;

  String ROOT_PATH = "/";
  String RELOCATED_SUB_PATH = "original";
  String LIB64_SUB_PATH = "usr/lib64";
  String LIB_POSTGRESQL_SUB_PATH = "usr/lib/postgresql";
  String LIB_SUB_PATH = "lib";
  String BIN_SUB_PATH = "bin";
  String SHARE_POSTGRESQL_SUB_PATH = "usr/share/postgresql";
  String EXTENSION_SUB_PATH = "extension";

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
        .flatMap(t -> t.v2.getAvailableFor().stream().map(target -> t.concat(target)))
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
        .flatMap(t -> t.v2.getAvailableFor().stream().map(target -> t.concat(target)))
        .map(t -> t.concat(new StackGresExtensionIndexSameMajorBuild(t.v1, t.v2, t.v3)))
        .grouped(t -> t.v4())
        .collect(Collectors.toMap(
            t -> t.v1,
            t -> t.v2.map(tt -> new StackGresExtensionMetadata(tt.v1, tt.v2, tt.v3,
                currentExtensionsMetadata.getPublishers().stream()
                .filter(publisher -> publisher.getId().equals(tt.v1.getPublisherOrDefault()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Can not find available version of extension "
                    + getDescription(tt.v1, tt.v2, tt.v3)))))
            .collect(ImmutableList.toImmutableList())));
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
        .flatMap(t -> t.v2.getAvailableFor().stream().map(target -> t.concat(target)))
        .map(t -> t.concat(new StackGresExtensionIndexAnyVersion(t.v1, t.v3)))
        .grouped(t -> t.v4())
        .toMap(
            t -> t.v1,
            t -> t.v2.map(tt -> new StackGresExtensionMetadata(tt.v1, tt.v2, tt.v3,
                currentExtensionsMetadata.getPublishers().stream()
                .filter(publisher -> publisher.getId().equals(tt.v1.getPublisherOrDefault()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Can not find available version of extension "
                    + getDescription(tt.v1, tt.v2, tt.v3)))))
            .collect(ImmutableList.toImmutableList()));
  }

  static Map<String, StackGresExtensionPublisher> toPublishersIndex(
      StackGresExtensions extensions) {
    return Seq.seq(extensions.getPublishers())
        .collect(ImmutableMap.toImmutableMap(
            StackGresExtensionPublisher::getId, Function.identity()));
  }

  static String getExtensionPackageName(
      StackGresClusterInstalledExtension extension) {
    final Optional<String> buildVersion = Optional.ofNullable(
        extension.getBuild());
    return extension.getName()
        + "-" + extension.getVersion()
        + "-pg" + extension.getPostgresVersion()
        + buildVersion.map(build -> "-build-" + build).orElse("");
  }

  static URI getExtensionPackageUri(URI repositoryUri,
      StackGresClusterInstalledExtension installedExtension) {
    return UriBuilder.fromUri(repositoryUri)
        .path(installedExtension.getPublisher())
        .path(ARCH_X86_64).path(OS_LINUX)
        .path(getExtensionPackageName(installedExtension) + ".tar")
        .build();
  }

  static StackGresClusterInstalledExtension getInstalledExtension(
      StackGresClusterExtension extension, StackGresExtensionMetadata extensionMetadata) {
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
    installedExtension.setBuild(extensionMetadata.getTarget().getBuild());
    return installedExtension;
  }

  static Optional<URI> getExtensionRepositoryUri(StackGresClusterExtension extension,
      StackGresExtensionMetadata extensionMetadata) {
    return Optional.ofNullable(extension.getRepository())
        .or(() -> Optional.ofNullable(extensionMetadata.getExtension().getRepository()))
        .map(URI::create);
  }

  static URI getIndexUri(URI extensionsUrl) {
    return UriBuilder.fromUri(extensionsUrl).path("/v1/index.json").build();
  }

  static String getDescription(StackGresCluster cluster,
      StackGresClusterExtension extension) {
    final String pgMajorVersion = getPostgresFlavorComponent(cluster).findMajorVersion(
        cluster.getSpec().getPostgres().getVersion());
    return getDescription(pgMajorVersion, extension);
  }

  static String getDescription(StackGresClusterInstalledExtension extension) {
    return extension.getPublisher() + "/" + extension.getName()
      + " for version " + extension.getVersion()
      + "[" + extension.getPostgresVersion() + "/" + ARCH_X86_64 + "/" + OS_LINUX + "]";
  }

  static String getDescription(String pgMajorVersion,
      StackGresClusterExtension extension) {
    return extension.getPublisherOrDefault() + "/" + extension.getName()
        + " for version " + extension.getVersionOrDefaultChannel()
        + "[" + pgMajorVersion + "/" + ARCH_X86_64 + "/" + OS_LINUX + "]";
  }

  static String getDescription(StackGresExtensionMetadata extensionMetadata) {
    return getDescription(extensionMetadata.getExtension(),
        extensionMetadata.getVersion(), extensionMetadata.getTarget());
  }

  static String getDescription(StackGresExtension extension,
      StackGresExtensionVersion version, StackGresExtensionVersionTarget target) {
    return extension.getPublisherOrDefault() + "/" + extension.getName()
        + " for version " + version.getVersion()
        + "[" + target.getPostgresVersion()
        + "/" + target.getArchOrDefault()
        + "/" + target.getOsOrDefault() + "]";
  }

  static String getMajorBuildOrNull(String build) {
    if (build == null) {
      return null;
    }
    return build.split(Pattern.quote("."))[0];
  }

}
