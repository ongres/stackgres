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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
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
  private final String labelClusterPodsPath;
  private final String annotationServicesPath;
  private final String annotationReplicasServicePath;
  private final String annotationPrimaryServicePath;
  private final String annotationClusterPodsPath;
  private final String annotationAllResourcesPath;
  private final String coordinatorLabelsServices;
  private final String coordinatorLabelsClusterPods;
  private final String coordinatorAnnotationsAllResources;
  private final String coordinatorAnnotationsClusterPods;
  private final String coordinatorAnnotationsServices;
  private final String coordinatorAnnotationsPrimaryService;
  private final String coordinatorAnnotationsReplicasService;
  private final String shardLabelsServices;
  private final String shardLabelsClusterPods;
  private final String shardAnnotationsAllResources;
  private final String shardAnnotationsClusterPods;
  private final String shardAnnotationsServices;
  private final String shardAnnotationsPrimaryService;
  private final String shardAnnotationsReplicasService;

  public MetadataValidator() {
    this.labelServicesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "services");

    this.labelClusterPodsPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "clusterPods"
    );

    this.annotationServicesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "services"
    );

    this.annotationReplicasServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "replicasService"
    );

    this.annotationPrimaryServicePath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "primaryService"
    );

    this.annotationClusterPodsPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "clusterPods"
    );

    this.annotationAllResourcesPath = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "allResources"
    );

    this.coordinatorLabelsServices = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "services"
    );

    this.coordinatorLabelsClusterPods = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "clusterPods"
    );

    this.coordinatorAnnotationsAllResources = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "allResources"
    );

    this.coordinatorAnnotationsClusterPods = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "clusterPods"
    );

    this.coordinatorAnnotationsServices = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "services"
    );

    this.coordinatorAnnotationsPrimaryService = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "primaryService"
    );

    this.coordinatorAnnotationsReplicasService = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "coordinator",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "replicasService"
    );

    this.shardLabelsServices = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "services"
    );

    this.shardLabelsClusterPods = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "labels",
      StackGresClusterSpecLabels.class, "clusterPods"
    );

    this.shardAnnotationsAllResources = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "allResources"
    );

    this.shardAnnotationsClusterPods = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "clusterPods"
    );

    this.shardAnnotationsServices = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "services"
    );

    this.shardAnnotationsPrimaryService = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "primaryService"
    );

    this.shardAnnotationsReplicasService = getFieldPath(
      StackGresShardedCluster.class, "spec",
      StackGresShardedClusterSpec.class, "shards",
      StackGresClusterSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "replicasService"
    );
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case Operation.CREATE, Operation.UPDATE -> {

        final StackGresShardedCluster cluster = review.getRequest().getObject();

        final Optional<StackGresClusterSpecLabels> maybeLabels = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getMetadata)
            .map(StackGresClusterSpecMetadata::getLabels);

        final Optional<StackGresClusterSpecLabels> maybeCoordinatorLabels = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getCoordinator)
            .map(StackGresShardedClusterCoordinator::getMetadata)
            .map(StackGresClusterSpecMetadata::getLabels);

        final Optional<StackGresClusterSpecLabels> maybeShardsLabels = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getShards)
            .map(StackGresShardedClusterShards::getMetadata)
            .map(StackGresClusterSpecMetadata::getLabels);

        final Optional<StackGresClusterSpecAnnotations> maybeAnnotations = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getMetadata)
            .map(StackGresClusterSpecMetadata::getAnnotations);

        final Optional<StackGresClusterSpecAnnotations> maybeCoordinatorAnnotations = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getCoordinator)
            .map(StackGresShardedClusterCoordinator::getMetadata)
            .map(StackGresClusterSpecMetadata::getAnnotations);

        final Optional<StackGresClusterSpecAnnotations> maybeShardsAnnotations = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresShardedClusterSpec::getShards)
            .map(StackGresShardedClusterShards::getMetadata)
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
          for (var entry : clusterPods.entrySet()) {
            checkLabel(labelClusterPodsPath, entry.getKey(), entry.getValue());
          }
        }

        if (maybeCoordinatorLabels.isPresent()) {
          final StackGresClusterSpecLabels coordinatorLabels = maybeCoordinatorLabels.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(coordinatorLabels.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkLabel(coordinatorLabelsServices, entry.getKey(), entry.getValue());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(coordinatorLabels.getClusterPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkLabel(coordinatorLabelsClusterPods, entry.getKey(), entry.getValue());
          }
        }

        if (maybeShardsLabels.isPresent()) {
          final StackGresClusterSpecLabels shardsLabels = maybeShardsLabels.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(shardsLabels.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkLabel(shardLabelsServices, entry.getKey(), entry.getValue());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(shardsLabels.getClusterPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkLabel(shardLabelsClusterPods, entry.getKey(), entry.getValue());
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

        if (maybeCoordinatorAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations coordinatorAnnotations = maybeCoordinatorAnnotations.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(coordinatorAnnotations.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkAnnotation(coordinatorAnnotationsServices, entry.getKey());
          }

          final Map<String, String> replicasService =
              Objects.requireNonNullElseGet(coordinatorAnnotations.getReplicasService(), Map::of);
          for (var entry : replicasService.entrySet()) {
            checkAnnotation(coordinatorAnnotationsReplicasService, entry.getKey());
          }

          final Map<String, String> primaryService =
              Objects.requireNonNullElseGet(coordinatorAnnotations.getPrimaryService(), Map::of);
          for (var entry : primaryService.entrySet()) {
            checkAnnotation(coordinatorAnnotationsPrimaryService, entry.getKey());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(coordinatorAnnotations.getClusterPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkAnnotation(coordinatorAnnotationsClusterPods, entry.getKey());
          }

          final Map<String, String> allResources =
              Objects.requireNonNullElseGet(coordinatorAnnotations.getAllResources(), Map::of);
          for (var entry : allResources.entrySet()) {
            checkAnnotation(coordinatorAnnotationsAllResources, entry.getKey());
          }
        }

        if (maybeShardsAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations shardsAnnotations = maybeShardsAnnotations.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(shardsAnnotations.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkAnnotation(shardAnnotationsServices, entry.getKey());
          }

          final Map<String, String> replicasService =
              Objects.requireNonNullElseGet(shardsAnnotations.getReplicasService(), Map::of);
          for (var entry : replicasService.entrySet()) {
            checkAnnotation(shardAnnotationsReplicasService, entry.getKey());
          }

          final Map<String, String> primaryService =
              Objects.requireNonNullElseGet(shardsAnnotations.getPrimaryService(), Map::of);
          for (var entry : primaryService.entrySet()) {
            checkAnnotation(shardAnnotationsPrimaryService, entry.getKey());
          }

          final Map<String, String> clusterPods =
              Objects.requireNonNullElseGet(shardsAnnotations.getClusterPods(), Map::of);
          for (var entry : clusterPods.entrySet()) {
            checkAnnotation(shardAnnotationsClusterPods, entry.getKey());
          }

          final Map<String, String> allResources =
              Objects.requireNonNullElseGet(shardsAnnotations.getAllResources(), Map::of);
          for (var entry : allResources.entrySet()) {
            checkAnnotation(shardAnnotationsAllResources, entry.getKey());
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
