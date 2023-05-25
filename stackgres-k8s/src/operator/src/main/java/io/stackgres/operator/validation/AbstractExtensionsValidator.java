/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.ErrorType;
import io.stackgres.common.ExtensionTuple;
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
      case CREATE, UPDATE:
        validateExtensions(review);
        break;
      default:
        break;
    }
  }

  protected void validateExtensions(T review) throws ValidationFailed {

    ExtensionReview extensionReview = getExtensionReview(review);

    List<ExtensionTuple> defaultExtensions = extensionReview
        .getDefaultExtensions();

    List<ExtensionTuple> requiredExtensions =
        getRequiredExtensions(extensionReview, defaultExtensions);

    List<ExtensionTuple> toInstallExtensions =
        getExtensionsToInstall(extensionReview);

    final List<ExtensionTuple> missingExtensions = getMissingExtensions(
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
      List<ExtensionTuple> missingExtensions,
      Map<String, List<String>> candidateExtensionVersions) {
    return Seq.seq(missingExtensions)
        .map(missingExtension -> {
          final Set<String> availableVersions =
              Set.copyOf(candidateExtensionVersions.get(missingExtension.extensionName()));
          if (!availableVersions.isEmpty()) {
            return missingExtension.extensionName()
                + " (available " + String.join(", ", availableVersions) + ")";
          }
          return missingExtension.extensionName();
        })
        .toString(", ");
  }

  private ImmutableMap<String, List<String>> getCandidateExtensionVersions(
      ExtensionReview extensionReview, List<ExtensionTuple> missingExtensions) {
    final List<StackGresClusterExtension> requiredExtensions = extensionReview
        .getRequiredExtensions();
    return missingExtensions
        .stream()
        .map(missingExtension -> {
          final StackGresClusterExtension extension = requiredExtensions.stream()
              .filter(ext -> ext.getName().equals(missingExtension.extensionName()))
              .findAny()
              .orElseGet(() -> {
                StackGresClusterExtension ext = new StackGresClusterExtension();
                ext.setName(missingExtension.extensionName());
                missingExtension.extensionVersion().ifPresent(ext::setVersion);
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
      List<ExtensionTuple> toInstallExtensions) {
    return Seq.seq(requiredExtensions)
        .filter(requiredExtension -> toInstallExtensions.stream()
            .noneMatch(toInstallExtension -> requiredExtension.extensionName().equals(
                toInstallExtension.extensionName())))
        .sorted(ExtensionTuple::extensionName)
        .toList();
  }

  private List<ExtensionTuple> getExtensionsToInstall(ExtensionReview review) {
    return review.getToInstallExtensions()
        .stream()
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .toList();
  }

  private List<ExtensionTuple> getRequiredExtensions(
      ExtensionReview review, List<ExtensionTuple> defaultExtensions) {
    List<ExtensionTuple> requiredExtensions = review.getRequiredExtensions()
        .stream()
        .map(extension -> new ExtensionTuple(extension.getName(), extension.getVersion()))
        .filter(extension -> defaultExtensions.stream()
            .map(ExtensionTuple::extensionName).noneMatch(extension.extensionName()::equals))
        .toList();
    return Seq.seq(requiredExtensions)
        .append(defaultExtensions)
        .toList();
  }

  protected abstract void failValidation(String reason, String message) throws ValidationFailed;

  protected abstract ExtensionReview getExtensionReview(T review);

}
