/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.extension.StackGresExtensionVersion;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsContextAppender<C, T> {

  protected abstract ExtensionMetadataManager getExtensionMetadataManager();

  public void appendContext(
      C inputContext,
      T contextBuilder,
      String postgresVersion,
      String buildVersion,
      Optional<String> previousVersion,
      Optional<String> previousBuildVersion) {
    StackGresCluster cluster = getCluster(inputContext);
    List<StackGresClusterExtension> extensions = getExtensions(inputContext, postgresVersion, buildVersion);
    List<StackGresClusterInstalledExtension> missingDefaultExtensions =
        getDefaultExtensions(inputContext, postgresVersion, buildVersion)
        .stream()
        .filter(defaultExtension -> extensions.stream()
            .map(StackGresClusterExtension::getName)
            .noneMatch(defaultExtension.extensionName()::equals))
        .map(t -> t.extensionVersion()
            .flatMap(version -> getExtension(cluster, t.extensionName(), version))
            .or(() -> getExtension(cluster, t.extensionName())))
        .flatMap(Optional::stream)
        .toList();
    final List<StackGresClusterInstalledExtension> toInstallExtensions =
        Seq.seq(extensions)
        .map(extension -> findToInstallExtension(cluster, extension))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .append(missingDefaultExtensions)
        .toList();

    setToInstallExtensions(inputContext, toInstallExtensions);

    List<ExtensionTuple> defaultExtensions = getDefaultExtensions(inputContext, postgresVersion, buildVersion);

    List<ExtensionTuple> requiredExtensions = getRequiredExtensions(
        inputContext, postgresVersion, buildVersion, defaultExtensions);

    final List<ExtensionTuple> missingExtensions = getMissingExtensions(
        requiredExtensions, toInstallExtensions);

    if (!missingExtensions.isEmpty()) {
      Map<String, List<String>> candidateExtensionVersions = getCandidateExtensionVersions(
          inputContext, postgresVersion, buildVersion, cluster, missingExtensions);

      String missingExtensionsMessage = getMissingExtensionsMessage(missingExtensions,
          candidateExtensionVersions);
      if (missingExtensions.size() == 1) {
        throw new IllegalArgumentException(
            "Extension was not found: " + missingExtensionsMessage);
      } else {
        throw new IllegalArgumentException(
            "Some extensions were not found: " + missingExtensionsMessage);
      }
    }
  }

  private String getMissingExtensionsMessage(
      List<ExtensionTuple> missingExtensions,
      Map<String, List<String>> candidateExtensionVersions) {
    return Seq.seq(missingExtensions)
        .map(missingExtension -> {
          final List<String> availableVersions =
              Set.copyOf(candidateExtensionVersions.get(missingExtension.extensionName()))
              .stream()
              .sorted(Comparator.comparing(StackGresUtil::sortableVersion)
                  .reversed())
              .toList();
          if (!availableVersions.isEmpty()) {
            return missingExtension.extensionName()
                + missingExtension.extensionVersion().map(v -> " " + v).orElse("")
                + " (available " + String.join(", ", availableVersions) + ")";
          }
          return missingExtension.extensionName()
              + missingExtension.extensionVersion().map(v -> " " + v).orElse("");
        })
        .toString(", ");
  }

  private Map<String, List<String>> getCandidateExtensionVersions(
      C inputContext,
      String postgresVersion,
      String buildVersion,
      StackGresCluster cluster,
      List<ExtensionTuple> missingExtensions) {
    final List<StackGresClusterExtension> requiredExtensions =
        getExtensions(inputContext, postgresVersion, buildVersion);
    return missingExtensions
        .stream()
        .map(missingExtension -> {
          final StackGresClusterExtension extension = requiredExtensions.stream()
              .filter(requiredExtension -> requiredExtension.getName()
                  .equals(missingExtension.extensionName()))
              .findAny()
              .orElseGet(() -> {
                return new StackGresClusterExtensionBuilder()
                    .withName(missingExtension.extensionName())
                    .withVersion(missingExtension.extensionVersion().orElse(null))
                    .build();
              });

          var extensionMetadataManager = getExtensionMetadataManager();
          final List<StackGresExtensionMetadata> extensionsAnyVersion = extensionMetadataManager
              .getExtensionsAnyVersion(cluster, extension, false);

          var candidateExtensions = extensionsAnyVersion.stream()
              .map(extensionMetadata -> extensionMetadata.getVersion().getVersion())
              .toList();
          return Tuple.tuple(
              missingExtension.extensionName(),
              candidateExtensions
          );
        })
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
  }

  private List<ExtensionTuple> getMissingExtensions(
      List<ExtensionTuple> requiredExtensions,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    return Seq.seq(requiredExtensions)
        .filter(requiredExtension -> toInstallExtensions.stream()
            .noneMatch(toInstallExtension -> requiredExtension.extensionName().equals(
                toInstallExtension.getName())))
        .sorted(ExtensionTuple::extensionName)
        .toList();
  }

  private List<ExtensionTuple> getRequiredExtensions(
      C inputContext,
      String postgresVersoin,
      String buildVersion,
      List<ExtensionTuple> defaultExtensions) {
    return Seq.seq(getExtensions(inputContext, postgresVersoin, buildVersion))
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .filter(extension -> defaultExtensions.stream()
            .map(ExtensionTuple::extensionName).noneMatch(extension.extensionName()::equals))
        .append(defaultExtensions)
        .toList();
  }

  protected abstract StackGresCluster getCluster(C inputContext);

  protected abstract List<ExtensionTuple> getDefaultExtensions(
      C inputContext, String version, String buildVersion);

  protected abstract List<StackGresClusterExtension> getExtensions(
      C inputContext, String version, String buildVersion);

  protected abstract void setToInstallExtensions(C inputContext,
      List<StackGresClusterInstalledExtension> toInstallExtensions);

  private Optional<StackGresClusterInstalledExtension> getExtension(StackGresCluster cluster,
      String extensionName) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    return getExtensionMetadataManager()
        .findExtensionCandidateAnyVersion(cluster, extension, false)
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            cluster, extension, extensionMetadata, false));
  }

  private Optional<StackGresClusterInstalledExtension> getExtension(
      StackGresCluster cluster,
      String extensionName,
      String extensionVersion) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    extension.setVersion(extensionVersion);
    return getExtensionMetadataManager()
        .findExtensionCandidateSameMajorBuild(cluster, extension, false)
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            cluster, extension, extensionMetadata, false));
  }

  private Optional<StackGresClusterInstalledExtension> findToInstallExtension(
      StackGresCluster cluster,
      StackGresClusterExtension extension) {
    return getExtensionMetadataManager()
        .findExtensionCandidateSameMajorBuild(cluster, extension, false)
        .or(() -> Optional.of(getExtensionMetadataManager()
            .getExtensionsAnyVersion(cluster, extension, false))
            .stream()
            .filter(list -> list.size() >= 1)
            .flatMap(List::stream)
            .filter(foundExtension -> foundExtension
                .getTarget().getPostgresVersion().contains("."))
            .findFirst())
        .or(() -> Optional.of(extension.getVersion() == null)
            .filter(hasNoVersion -> hasNoVersion)
            .map(hasNoVersion -> getExtensionMetadataManager()
                .getExtensionsAnyVersion(cluster, extension, false))
            .filter(Predicates.not(List::isEmpty))
            .filter(allExtensionVersions -> Seq.seq(allExtensionVersions)
                .groupBy(Function.<StackGresExtensionMetadata>identity()
                    .andThen(StackGresExtensionMetadata::getVersion)
                    .andThen(StackGresExtensionVersion::getVersion))
                .size() >= 1)
            .map(List::stream)
            .flatMap(Stream::findFirst))
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            cluster, extension, extensionMetadata, false));
  }

}
