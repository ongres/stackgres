/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecAnnotations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class MetadataValidator implements ShardedClusterValidator {

  private final String labelServicesPath;
  private final String labelReplicasServicePath;
  private final String labelPrimaryServicePath;
  private final String labelClusterPodsPath;
  private final String labelServiceAccountPath;
  private final String labelAllResourcesPath;
  private final String labelCoordinatorPrimaryServicePath;
  private final String labelCoordinatorAnyServicePath;
  private final String labelShardsPrimariesServicePath;
  private final String annotationServicesPath;
  private final String annotationReplicasServicePath;
  private final String annotationPrimaryServicePath;
  private final String annotationClusterPodsPath;
  private final String annotationServiceAccountPath;
  private final String annotationAllResourcesPath;
  private final String annotationCoordinatorPrimaryServicePath;
  private final String annotationCoordinatorAnyServicePath;
  private final String annotationShardsPrimariesServicePath;

  public MetadataValidator() {
    this.labelServicesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "services"
    );

    this.labelReplicasServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "replicasService"
    );

    this.labelPrimaryServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "primaryService"
    );

    this.labelClusterPodsPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "clusterPods"
    );

    this.labelServiceAccountPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "serviceAccount"
    );

    this.labelAllResourcesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "allResources"
    );

    this.labelCoordinatorPrimaryServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresShardedClusterSpecLabels.class, "coordinatorPrimaryService"
    );

    this.labelCoordinatorAnyServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresShardedClusterSpecLabels.class, "coordinatorAnyService"
    );

    this.labelShardsPrimariesServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "labels",
      StackGresShardedClusterSpecLabels.class, "shardsPrimariesService"
    );

    this.annotationServicesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "services"
    );

    this.annotationReplicasServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "replicasService"
    );

    this.annotationPrimaryServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "primaryService"
    );

    this.annotationClusterPodsPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "clusterPods"
    );

    this.annotationServiceAccountPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "serviceAccount"
    );

    this.annotationAllResourcesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "allResources"
    );

    this.annotationCoordinatorPrimaryServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresShardedClusterSpecAnnotations.class, "coordinatorPrimaryService"
    );

    this.annotationCoordinatorAnyServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresShardedClusterSpecAnnotations.class, "coordinatorAnyService"
    );

    this.annotationShardsPrimariesServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresShardedClusterSpecMetadata.class, "annotations",
      StackGresShardedClusterSpecAnnotations.class, "shardsPrimariesService"
    );
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case Operation.CREATE, Operation.UPDATE -> {

        final StackGresShardedCluster cluster = review.getRequest().getObject();

        final Optional<StackGresShardedClusterSpecLabels> maybeLabels = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getMetadata)
            .map(StackGresShardedClusterSpecMetadata::getLabels);

        final Optional<StackGresShardedClusterSpecAnnotations> maybeAnnotations = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getMetadata)
            .map(StackGresShardedClusterSpecMetadata::getAnnotations);

        if (maybeLabels.isPresent()) {
          final StackGresShardedClusterSpecLabels labels = maybeLabels.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(labels.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkLabel(labelServicesPath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> replicasService =
              Objects.requireNonNullElseGet(labels.getReplicasService(), Map::of);
          for (var entry : replicasService.entrySet()) {
            checkLabel(labelReplicasServicePath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> primaryService =
              Objects.requireNonNullElseGet(labels.getPrimaryService(), Map::of);
          for (var entry : primaryService.entrySet()) {
            checkLabel(labelPrimaryServicePath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(labels.getClusterPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkLabel(labelClusterPodsPath, entry.getKey(), entry.getValue());
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

          final Map<String, String> coordinatorPrimaryService =
              Objects.requireNonNullElseGet(labels.getCoordinatorPrimaryService(), Map::of);
          for (var entry : coordinatorPrimaryService.entrySet()) {
            checkLabel(labelCoordinatorPrimaryServicePath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> coordinatorAnyService =
              Objects.requireNonNullElseGet(labels.getCoordinatorAnyService(), Map::of);
          for (var entry : coordinatorAnyService.entrySet()) {
            checkLabel(labelCoordinatorAnyServicePath, entry.getKey(), entry.getValue());
          }

          final Map<String, String> shardsPrimariesService =
              Objects.requireNonNullElseGet(labels.getShardsPrimariesService(), Map::of);
          for (var entry : shardsPrimariesService.entrySet()) {
            checkLabel(labelShardsPrimariesServicePath, entry.getKey(), entry.getValue());
          }
        }

        if (maybeAnnotations.isPresent()) {
          final StackGresShardedClusterSpecAnnotations annotations = maybeAnnotations.get();

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

          final Map<String, String> coordinatorPrimaryService =
              Objects.requireNonNullElseGet(annotations.getAllResources(), Map::of);
          for (var entry : coordinatorPrimaryService.entrySet()) {
            checkAnnotation(annotationCoordinatorPrimaryServicePath, entry.getKey());
          }

          final Map<String, String> coordinatorAnyService =
              Objects.requireNonNullElseGet(annotations.getAllResources(), Map::of);
          for (var entry : coordinatorAnyService.entrySet()) {
            checkAnnotation(annotationCoordinatorAnyServicePath, entry.getKey());
          }

          final Map<String, String> shardsPrimariesService =
              Objects.requireNonNullElseGet(annotations.getAllResources(), Map::of);
          for (var entry : shardsPrimariesService.entrySet()) {
            checkAnnotation(annotationShardsPrimariesServicePath, entry.getKey());
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
          HasMetadata.getKind(StackGresShardedCluster.class),
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
          HasMetadata.getKind(StackGresShardedCluster.class),
          ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION),
          e.getMessage(),
          String.format("%s.%s", basePath, key),
          basePath
      );
    }
  }
}
