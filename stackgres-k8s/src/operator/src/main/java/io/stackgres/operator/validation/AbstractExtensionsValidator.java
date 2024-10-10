/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.docir.DocirExtensionMetadata;
import io.stackgres.common.docir.DocirExtensionVersion;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.extension.StackGresExtensionVersion;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsValidator<
      R extends HasMetadata, T extends AdmissionReview<R>>
    implements Validator<T> {

  protected abstract StackGresContext getContext();

  @Override
  public void validate(T review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE, UPDATE:
        final StackGresCluster cluster = getCluster(review);
        final StackGresCluster oldCluster = getOldCluster(review);
        if (extensionsChanged(review, cluster, oldCluster)) {
          validateExtensions(review, cluster);
        }
        break;
      default:
        break;
    }
  }

  protected boolean extensionsChanged(
      T review,
      StackGresCluster cluster,
      StackGresCluster oldCluster) {
    if (majorVersionOrBuildVersionChanged(review, cluster, oldCluster)) {
      return true;
    }
    final R resource = review.getRequest().getObject();
    final R oldResource = review.getRequest().getOldObject();
    if (oldResource == null) {
      return true;
    }
    final List<StackGresClusterExtension> extensions =
        getExtensions(resource, cluster);
    final List<StackGresClusterExtension> oldExtensions =
        getExtensions(oldResource, oldCluster);
    if (!Objects.equals(extensions, oldExtensions)) {
      return true;
    }
    final List<ExtensionTuple> missingDefaultExtensions =
        getDefaultExtensions(resource, cluster);
    final List<ExtensionTuple> oldMissingDefaultExtensions =
        getDefaultExtensions(oldResource, oldCluster);
    if (!Objects.equals(missingDefaultExtensions, oldMissingDefaultExtensions)) {
      return true;
    }
    final Optional<List<StackGresClusterInstalledExtension>> toInstallExtensions =
        getToInstallExtensions(resource);
    final Optional<List<StackGresClusterInstalledExtension>> oldToInstallExtensions =
        getToInstallExtensions(oldResource);
    if (!Objects.equals(toInstallExtensions, oldToInstallExtensions)) {
      return true;
    }
    return false;
  }

  private boolean majorVersionOrBuildVersionChanged(
      T review,
      StackGresCluster cluster,
      StackGresCluster oldCluster) {
    if (review.getRequest().getOldObject() == null) {
      return true;
    }
    String postgresVersion = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(null);
    String oldPostgresVersion = Optional.of(oldCluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(null);
    String postgresMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getMajorVersion(getContext(), postgresVersion);
    String oldPostgresMajorVersion = getPostgresFlavorComponent(oldCluster)
        .get(oldCluster)
        .getMajorVersion(getContext(), oldPostgresVersion);
    if (!Objects.equals(postgresMajorVersion, oldPostgresMajorVersion)) {
      return true;
    }
    if (!StackGresUtil.isRegistryEnabled(cluster)) {
      String buildMajorVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getBuildMajorVersion(getContext(), postgresVersion);
      String oldBuildMajorVersion = getPostgresFlavorComponent(oldCluster)
          .get(oldCluster)
          .getBuildMajorVersion(getContext(), oldPostgresVersion);
      if (!Objects.equals(buildMajorVersion, oldBuildMajorVersion)) {
        return true;
      }
    } else {
      String buildVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getBuildVersion(getContext(), postgresVersion);
      String oldBuildVersion = getPostgresFlavorComponent(oldCluster)
          .get(oldCluster)
          .getBuildVersion(getContext(), oldPostgresVersion);
      if (!Objects.equals(buildVersion, oldBuildVersion)) {
        return true;
      }
    }
    return false;
  }

  protected void validateExtensions(
      T review,
      StackGresCluster cluster) throws ValidationFailed {
    final R resource = review.getRequest().getObject();

    List<ExtensionTuple> defaultExtensions = getDefaultExtensions(resource, cluster);

    List<ExtensionTuple> requiredExtensions = getRequiredExtensions(
        resource, cluster, defaultExtensions);

    List<ExtensionTuple> toInstallExtensions = getToInstallExtensions(resource)
        .stream()
        .flatMap(List::stream)
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .toList();

    final List<ExtensionTuple> missingExtensions = getMissingExtensions(
        requiredExtensions, toInstallExtensions);

    if (!missingExtensions.isEmpty()) {
      Map<String, List<String>> candidateExtensionVersions = getCandidateExtensionVersions(
          resource, cluster, missingExtensions);

      String errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.EXTENSION_NOT_FOUND);
      String missingExtensionsMessage = getMissingExtensionsMessage(missingExtensions,
          candidateExtensionVersions);
      if (missingExtensions.size() == 1) {
        failValidation(errorTypeUri,
            "Extension was not found: " + missingExtensionsMessage);
      } else {
        failValidation(errorTypeUri,
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
      R resource,
      StackGresCluster cluster,
      List<ExtensionTuple> missingExtensions) {
    final List<StackGresClusterExtension> requiredExtensions = getExtensions(resource, cluster);
    if (!StackGresUtil.isRegistryEnabled(cluster)) {
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
  
            final List<StackGresExtensionMetadata> extensionsAnyVersion = getExtensionMetadataManager()
                .getExtensionsAnyVersion(cluster, extension, false);
  
            final List<String> candidateExtensions = Seq.seq(extensionsAnyVersion)
                .grouped(Function.<StackGresExtensionMetadata>identity()
                    .andThen(StackGresExtensionMetadata::getVersion)
                    .andThen(StackGresExtensionVersion::getVersion))
                .map(Tuple2::v1)
                .sorted(Comparator.comparing(
                    Function.<String>identity()
                    .andThen(StackGresUtil::sortableVersion)).reversed())
                .toList();
            return Tuple.tuple(
                missingExtension.extensionName(),
                candidateExtensions
            );
          })
          .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
    }
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

          final List<DocirExtensionMetadata> extensionsAnyVersion = getContext()
              .getMetadataManager()
              .getExtensionsAnyVersion(getContext(), cluster, extension, false);

          final List<String> candidateExtensions = Seq.seq(extensionsAnyVersion)
              .grouped(Function.<DocirExtensionMetadata>identity()
                  .andThen(DocirExtensionMetadata::getVersion)
                  .andThen(DocirExtensionVersion::getVersion))
              .map(Tuple2::v2)
              .map(Seq::findFirst)
              .flatMap(Optional::stream)
              .sorted(Comparator.comparing(
                  Function.<DocirExtensionMetadata>identity()
                  .andThen(DocirExtensionMetadata::getSortableVersion)).reversed())
              .map(Function.<DocirExtensionMetadata>identity()
                  .andThen(DocirExtensionMetadata::getVersion)
                  .andThen(DocirExtensionVersion::getVersion))
              .toList();
          return Tuple.tuple(
              missingExtension.extensionName(),
              candidateExtensions
          );
        })
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
  }

  protected abstract ExtensionMetadataManager getExtensionMetadataManager();

  private List<ExtensionTuple> getMissingExtensions(
      List<ExtensionTuple> requiredExtensions,
      List<ExtensionTuple> toInstallExtensions) {
    return Seq.seq(requiredExtensions)
        .filter(requiredExtension -> toInstallExtensions.stream()
            .noneMatch(toInstallExtension -> requiredExtension.extensionName().equals(
                toInstallExtension.extensionName())))
        .sorted(ExtensionTuple::extensionName)
        .toList();
  }

  private List<ExtensionTuple> getRequiredExtensions(
      R resource,
      StackGresCluster cluster,
      List<ExtensionTuple> defaultExtensions) {
    return Seq.seq(getExtensions(resource, cluster))
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .filter(extension -> defaultExtensions.stream()
            .map(ExtensionTuple::extensionName).noneMatch(extension.extensionName()::equals))
        .append(defaultExtensions)
        .toList();
  }

  protected abstract StackGresCluster getCluster(T resource) throws ValidationFailed;

  protected abstract StackGresCluster getOldCluster(T resource) throws ValidationFailed;

  protected abstract List<StackGresClusterExtension> getExtensions(
      R resource,
      StackGresCluster cluster);

  protected abstract List<ExtensionTuple> getDefaultExtensions(
      R resource,
      StackGresCluster cluster);

  protected abstract Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      R resource);

  protected abstract void failValidation(String reason, String message) throws ValidationFailed;

}
