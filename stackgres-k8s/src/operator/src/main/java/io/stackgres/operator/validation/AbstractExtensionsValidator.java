/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.ExtensionRequest;
import io.stackgres.common.extension.ImmutableExtensionRequest;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsValidator<T extends AdmissionReview<?>>
    implements Validator<T> {

  protected abstract ExtensionMetadataManager getExtensionMetadataManager();

  @Override
  public void validate(T review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
      case UPDATE: {

        validateExtensions(review);
        break;
      }
      default:
    }
  }

  protected void validateExtensions(T review) throws ValidationFailed {

    ExtensionReview extensionReview = getExtensionReview(review);

    List<Tuple2<String, Optional<String>>> defaultExtensions = extensionReview
        .getDefaultExtensions();

    List<Tuple2<String, Optional<String>>> requiredExtensions =
        getRequiredExtensions(extensionReview, defaultExtensions);

    List<Tuple2<String, Optional<String>>> toInstallExtensions =
        getExtensionsToInstall(extensionReview);

    final List<Tuple2<String, Optional<String>>> missingExtensions = getMissingExtensions(
        requiredExtensions, toInstallExtensions);
    if (!missingExtensions.isEmpty()) {
      Map<String, List<String>> candidateExtensionVersions = getCandidateExtensionVersions(
          extensionReview, missingExtensions);
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
      List<Tuple2<String, Optional<String>>> missingExtensions,
      Map<String, List<String>> candidateExtensionVersions) {
    return Seq.seq(missingExtensions)
        .map(missingExtension -> {
          final Set<String> availableVersions =
              Set.copyOf(candidateExtensionVersions.get(missingExtension.v1));
          if (!availableVersions.isEmpty()) {
            return missingExtension.v1
                + " (available " + Seq.seq(availableVersions).toString(", ") + ")";
          }
          return missingExtension.v1;
        })
        .toString(", ");
  }

  private ImmutableMap<String, List<String>> getCandidateExtensionVersions(
      ExtensionReview extensionReview,
      List<Tuple2<String, Optional<String>>> missingExtensions) {
    final List<StackGresClusterExtension> requiredExtensions = extensionReview
        .getRequiredExtensions();
    return missingExtensions
        .stream()
        .map(missingExtension -> {
          final StackGresClusterExtension extension = requiredExtensions.stream()
              .filter(ext -> ext.getName().equals(
                  missingExtension.v1))
              .findAny()
              .orElseGet(() -> {
                StackGresClusterExtension ext = new StackGresClusterExtension();
                ext.setName(missingExtension.v1);
                missingExtension.v2.ifPresent(ext::setVersion);
                return ext;
              });

          ExtensionRequest er = ImmutableExtensionRequest
              .builder()
              .arch(extensionReview.getArch())
              .os(extensionReview.getOs())
              .postgresVersion(extensionReview.getPostgresVersion())
              .stackGresComponent(extensionReview.getPostgresFlavor())
              .extension(extension)
              .stackGresVersion(extensionReview.getStackGresVersion())
              .build();

          var extensionMetadataManager = getExtensionMetadataManager();
          final List<StackGresExtensionMetadata> extensionsAnyVersion = extensionMetadataManager
              .requestExtensionsAnyVersion(er, false);

          var candidateExtensions = extensionsAnyVersion.stream()
              .map(extensionMetadata -> extensionMetadata.getVersion().getVersion())
              .collect(Collectors.toUnmodifiableList());
          return Tuple.tuple(
              missingExtension.v1,
              candidateExtensions
          );
        })
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
  }

  private List<Tuple2<String, Optional<String>>> getMissingExtensions(
      List<Tuple2<String, Optional<String>>> requiredExtensions,
      List<Tuple2<String, Optional<String>>> toInstallExtensions) {
    return Seq.seq(requiredExtensions)
        .filter(requiredExtension -> toInstallExtensions.stream()
            .noneMatch(toInstallExtension -> requiredExtension.v1.equals(
                toInstallExtension.v1)))
        .sorted(Tuple2::v1)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Tuple2<String, Optional<String>>> getExtensionsToInstall(ExtensionReview review) {

    return review.getToInstallExtensions()
        .stream()
        .map(extension -> Tuple.tuple(
            extension.getName(), Optional.ofNullable(extension.getVersion())))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Tuple2<String, Optional<String>>> getRequiredExtensions(
      ExtensionReview review,
      List<Tuple2<String, Optional<String>>> defaultExtensions
  ) {
    List<Tuple2<String, Optional<String>>> requiredExtensions = review.getRequiredExtensions()
        .stream()
        .map(extension -> Tuple.tuple(
            extension.getName(), Optional.ofNullable(extension.getVersion())
        ))
        .filter(extension -> defaultExtensions.stream()
            .map(Tuple2::v1).noneMatch(extension.v1::equals))
        .collect(Collectors.toUnmodifiableList());
    return Seq.seq(requiredExtensions)
        .append(defaultExtensions)
        .collect(Collectors.toUnmodifiableList());
  }

  protected abstract void failValidation(String reason, String message) throws ValidationFailed;

  protected abstract ExtensionReview getExtensionReview(T review);

}
