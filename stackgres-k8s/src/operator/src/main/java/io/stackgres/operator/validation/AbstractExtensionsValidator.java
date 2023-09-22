/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsValidator<
      R extends HasMetadata, T extends AdmissionReview<R>>
    implements Validator<T> {

  @Override
  public void validate(T review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE, UPDATE:
        if (extensionsChanged(review)) {
          validateExtensions(review);
        }
        break;
      default:
        break;
    }
  }

  protected boolean extensionsChanged(T review) {
    if (majorVersionOrBuildVersionChanged(review)) {
      return true;
    }
    final R resource = review.getRequest().getObject();
    final R oldResource = review.getRequest().getOldObject();
    if (oldResource == null) {
      return true;
    }
    final List<StackGresClusterExtension> extensions = getExtensions(resource);
    final List<StackGresClusterExtension> oldExtensions = getExtensions(oldResource);
    if (!Objects.equals(extensions, oldExtensions)) {
      return true;
    }
    final List<ExtensionTuple> missingDefaultExtensions = getDefaultExtensions(resource);
    final List<ExtensionTuple> oldMissingDefaultExtensions = getDefaultExtensions(oldResource);
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

  private boolean majorVersionOrBuildVersionChanged(T review) {
    final R resource = review.getRequest().getObject();
    final R oldResource = review.getRequest().getOldObject();
    if (oldResource == null) {
      return true;
    }
    final StackGresCluster cluster = getCluster(resource);
    final StackGresCluster oldCluster = getCluster(oldResource);
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
        .getMajorVersion(postgresVersion);
    String oldPostgresMajorVersion = getPostgresFlavorComponent(oldCluster)
        .get(oldCluster)
        .getMajorVersion(oldPostgresVersion);
    if (!Objects.equals(postgresMajorVersion, oldPostgresMajorVersion)) {
      return true;
    }
    String buildMajorVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getBuildMajorVersion(postgresVersion);
    String oldBuildMajorVersion = getPostgresFlavorComponent(oldCluster)
        .get(oldCluster)
        .getBuildMajorVersion(oldPostgresVersion);
    if (!Objects.equals(buildMajorVersion, oldBuildMajorVersion)) {
      return true;
    }
    return false;
  }

  protected void validateExtensions(T review) throws ValidationFailed {
    final R resource = review.getRequest().getObject();

    List<ExtensionTuple> defaultExtensions = getDefaultExtensions(resource);

    List<ExtensionTuple> requiredExtensions = getRequiredExtensions(resource, defaultExtensions);

    List<ExtensionTuple> toInstallExtensions = getToInstallExtensions(resource)
        .stream()
        .flatMap(List::stream)
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .toList();

    final List<ExtensionTuple> missingExtensions = getMissingExtensions(
        requiredExtensions, toInstallExtensions);

    if (!missingExtensions.isEmpty()) {
      Map<String, List<String>> candidateExtensionVersions = getCandidateExtensionVersions(
          resource, missingExtensions);

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
          final Set<String> availableVersions =
              Set.copyOf(candidateExtensionVersions.get(missingExtension.extensionName()));
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
      R resource, List<ExtensionTuple> missingExtensions) {
    final List<StackGresClusterExtension> requiredExtensions = getExtensions(resource);
    final StackGresCluster cluster = getCluster(resource);
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
      R resource, List<ExtensionTuple> defaultExtensions) {
    return Seq.seq(getExtensions(resource))
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .filter(extension -> defaultExtensions.stream()
            .map(ExtensionTuple::extensionName).noneMatch(extension.extensionName()::equals))
        .append(defaultExtensions)
        .toList();
  }

  protected abstract StackGresCluster getCluster(R resource);

  protected abstract List<StackGresClusterExtension> getExtensions(R resource);

  protected abstract List<ExtensionTuple> getDefaultExtensions(R resource);

  protected abstract Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      R resource);

  protected abstract void failValidation(String reason, String message) throws ValidationFailed;

}
