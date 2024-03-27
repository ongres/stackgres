/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class MetadataValidator implements ClusterValidator {

  private final String labelServicesPath;
  private final String labelClusterPodsPath;
  private final String annotationServicesPath;
  private final String annotationReplicasServicePath;
  private final String annotationPrimaryServicePath;
  private final String annotationClusterPodsPath;
  private final String annotationAllResourcesPath;

  public MetadataValidator() {
    this.labelServicesPath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "services");

    this.labelClusterPodsPath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "clusterPods"
    );

    this.annotationServicesPath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "services"
    );

    this.annotationReplicasServicePath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "replicasService"
    );

    this.annotationPrimaryServicePath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "primaryService"
    );

    this.annotationClusterPodsPath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "clusterPods"
    );

    this.annotationAllResourcesPath = getFieldPath(
      StackGresCluster.class, "spec",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "allResources"
    );
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case Operation.CREATE, Operation.UPDATE -> {

        final StackGresCluster cluster = review.getRequest().getObject();

        final Optional<StackGresClusterSpecLabels> maybeLabels = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getMetadata)
            .map(StackGresClusterSpecMetadata::getLabels);

        final Optional<StackGresClusterSpecAnnotations> maybeAnnotations = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getMetadata)
            .map(StackGresClusterSpecMetadata::getAnnotations);

        if (maybeLabels.isPresent()) {
          final StackGresClusterSpecLabels labels = maybeLabels.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(labels.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkLabel(labelServicesPath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(labels.getClusterPods(), Map::of);
          for (var entry: clusterPods.entrySet()) {
            checkLabel(labelClusterPodsPath, entry.getKey(), entry.getValue());
          }
        }

        if (maybeAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations annotations = maybeAnnotations.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(annotations.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkAnnotation(annotationServicesPath, entry.getKey());
          }

          final Map<String, String> replicasService =
              Objects.requireNonNullElseGet(annotations.getReplicasService(), Map::of);
          for (var entry : replicasService.entrySet()) {
            checkAnnotation(annotationReplicasServicePath, entry.getKey());
          }

          final Map<String, String> primaryService =
              Objects.requireNonNullElseGet(annotations.getPrimaryService(), Map::of);
          for (var entry : primaryService.entrySet()) {
            checkAnnotation(annotationPrimaryServicePath, entry.getKey());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(annotations.getClusterPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkAnnotation(annotationClusterPodsPath, entry.getKey());
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
          HasMetadata.getKind(StackGresCluster.class),
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
          HasMetadata.getKind(StackGresCluster.class),
          ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION),
          e.getMessage(),
          String.format("%s.%s", basePath, key),
          basePath
      );
    }
  }
}
