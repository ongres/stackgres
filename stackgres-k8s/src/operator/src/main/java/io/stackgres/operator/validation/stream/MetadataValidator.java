/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecAnnotations;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecLabels;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecMetadata;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class MetadataValidator implements StreamValidator {

  private final String labelPodsPath;
  private final String labelServiceAccountPath;
  private final String labelAllResourcesPath;
  private final String annotationPodsPath;
  private final String annotationServiceAccountPath;
  private final String annotationAllResourcesPath;

  public MetadataValidator() {
    this.labelPodsPath = getFieldPath(
      StackGresStream.class, "spec",
      StackGresStreamSpec.class, "metadata",
      StackGresStreamSpecMetadata.class, "labels",
      StackGresStreamSpecLabels.class, "pods"
    );

    this.labelServiceAccountPath = getFieldPath(
      StackGresStream.class, "spec",
      StackGresStreamSpec.class, "metadata",
      StackGresStreamSpecMetadata.class, "labels",
      StackGresStreamSpecLabels.class, "serviceAccount"
    );

    this.labelAllResourcesPath = getFieldPath(
      StackGresStream.class, "spec",
      StackGresStreamSpec.class, "metadata",
      StackGresStreamSpecMetadata.class, "labels",
      StackGresStreamSpecLabels.class, "allResources"
    );

    this.annotationPodsPath = getFieldPath(
      StackGresStream.class, "spec",
      StackGresStreamSpec.class, "metadata",
      StackGresStreamSpecMetadata.class, "annotations",
      StackGresStreamSpecAnnotations.class, "pods"
    );

    this.annotationServiceAccountPath = getFieldPath(
      StackGresStream.class, "spec",
      StackGresStreamSpec.class, "metadata",
      StackGresStreamSpecMetadata.class, "annotations",
      StackGresStreamSpecAnnotations.class, "serviceAccount"
    );

    this.annotationAllResourcesPath = getFieldPath(
      StackGresStream.class, "spec",
      StackGresStreamSpec.class, "metadata",
      StackGresStreamSpecMetadata.class, "annotations",
      StackGresStreamSpecAnnotations.class, "allResources"
    );
  }

  @Override
  public void validate(StackGresStreamReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case Operation.CREATE, Operation.UPDATE -> {

        final StackGresStream stream = review.getRequest().getObject();

        final Optional<StackGresStreamSpecLabels> maybeLabels = Optional
            .ofNullable(stream.getSpec())
            .map(StackGresStreamSpec::getMetadata)
            .map(StackGresStreamSpecMetadata::getLabels);

        final Optional<StackGresStreamSpecAnnotations> maybeAnnotations = Optional
            .ofNullable(stream.getSpec())
            .map(StackGresStreamSpec::getMetadata)
            .map(StackGresStreamSpecMetadata::getAnnotations);

        if (maybeLabels.isPresent()) {
          final StackGresStreamSpecLabels labels = maybeLabels.get();

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(labels.getPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkLabel(labelPodsPath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> serviceAccount =
              Objects.requireNonNullElseGet(labels.getServiceAccount(), Map::of);
          for (var entry : serviceAccount.entrySet()) {
            checkLabel(labelServiceAccountPath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> allResources =
              Objects.requireNonNullElseGet(labels.getAllResources(), Map::of);
          for (var entry : allResources.entrySet()) {
            checkLabel(labelAllResourcesPath, entry.getKey(), entry.getValue());
          }
        }

        if (maybeAnnotations.isPresent()) {
          final StackGresStreamSpecAnnotations annotations = maybeAnnotations.get();

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(annotations.getPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkAnnotation(annotationPodsPath, entry.getKey());
          }

          final Map<String, String> serviceAccount =
              Objects.requireNonNullElseGet(annotations.getAllResources(), Map::of);
          for (var entry : serviceAccount.entrySet()) {
            checkAnnotation(annotationServiceAccountPath, entry.getKey());
          }

          final Map<String, String> allResources =
              Objects.requireNonNullElseGet(annotations.getAllResources(), Map::of);
          for (var entry : allResources.entrySet()) {
            checkAnnotation(annotationAllResourcesPath, entry.getKey());
          }
        }
      }
      default -> { }
    }
  }

  private void checkLabel(String basePath, String key, String value) throws ValidationFailed {
    try {
      ResourceUtil.labelKeySyntax(key);
      ResourceUtil.labelValue(value);
    } catch (IllegalArgumentException e) {
      failWithMessageAndFields(
          HasMetadata.getKind(StackGresStream.class),
          ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION),
          e.getMessage(),
          String.format("%s.%s", basePath, key),
          basePath
      );
    }
  }

  private void checkAnnotation(String basePath, String key) throws ValidationFailed {
    try {
      ResourceUtil.annotationKeySyntax(key);
    } catch (IllegalArgumentException e) {
      failWithMessageAndFields(
          HasMetadata.getKind(StackGresStream.class),
          ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION),
          e.getMessage(),
          String.format("%s.%s", basePath, key),
          basePath
      );
    }
  }
}
