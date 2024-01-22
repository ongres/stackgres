/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

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

          for (var entry : labels.getServices().entrySet()) {
            checkLabel(labelServicesPath, entry.getKey(), entry.getValue());
          }
          for (var entry : labels.getClusterPods().entrySet()) {
            checkLabel(labelClusterPodsPath, entry.getKey(), entry.getValue());
          }
        }

        if (maybeCoordinatorLabels.isPresent()) {
          final StackGresClusterSpecLabels coordinatorLabels = maybeCoordinatorLabels.get();

          for (var entry : coordinatorLabels.getServices().entrySet()) {
            checkLabel(coordinatorLabelsServices, entry.getKey(), entry.getValue());
          }

          for (var entry : coordinatorLabels.getClusterPods().entrySet()) {
            checkLabel(coordinatorLabelsClusterPods, entry.getKey(), entry.getValue());
          }
        }

        if (maybeShardsLabels.isPresent()) {
          final StackGresClusterSpecLabels shardsLabels = maybeShardsLabels.get();

          for (var entry : shardsLabels.getServices().entrySet()) {
            checkLabel(shardLabelsServices, entry.getKey(), entry.getValue());
          }

          for (var entry : shardsLabels.getClusterPods().entrySet()) {
            checkLabel(shardLabelsClusterPods, entry.getKey(), entry.getValue());
          }
        }

        if (maybeAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations annotations = maybeAnnotations.get();

          for (var entry : annotations.getServices().entrySet()) {
            checkAnnotation(annotationServicesPath, entry.getKey());
          }
          for (var entry : annotations.getReplicasService().entrySet()) {
            checkAnnotation(annotationReplicasServicePath, entry.getKey());
          }
          for (var entry : annotations.getPrimaryService().entrySet()) {
            checkAnnotation(annotationPrimaryServicePath, entry.getKey());
          }
          for (var entry : annotations.getClusterPods().entrySet()) {
            checkAnnotation(annotationClusterPodsPath, entry.getKey());
          }
          for (var entry : annotations.getAllResources().entrySet()) {
            checkAnnotation(annotationAllResourcesPath, entry.getKey());
          }
        }

        if (maybeCoordinatorAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations coordinatorAnnotations = maybeCoordinatorAnnotations.get();

          for (var entry : coordinatorAnnotations.getServices().entrySet()) {
            checkAnnotation(coordinatorAnnotationsServices, entry.getKey());
          }
          for (var entry : coordinatorAnnotations.getReplicasService().entrySet()) {
            checkAnnotation(coordinatorAnnotationsReplicasService, entry.getKey());
          }
          for (var entry : coordinatorAnnotations.getPrimaryService().entrySet()) {
            checkAnnotation(coordinatorAnnotationsPrimaryService, entry.getKey());
          }
          for (var entry : coordinatorAnnotations.getClusterPods().entrySet()) {
            checkAnnotation(coordinatorAnnotationsClusterPods, entry.getKey());
          }
          for (var entry : coordinatorAnnotations.getAllResources().entrySet()) {
            checkAnnotation(coordinatorAnnotationsAllResources, entry.getKey());
          }
        }

        if (maybeShardsAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations shardsAnnotations = maybeShardsAnnotations.get();

          for (var entry : shardsAnnotations.getServices().entrySet()) {
            checkAnnotation(shardAnnotationsServices, entry.getKey());
          }
          for (var entry : shardsAnnotations.getReplicasService().entrySet()) {
            checkAnnotation(shardAnnotationsReplicasService, entry.getKey());
          }
          for (var entry : shardsAnnotations.getPrimaryService().entrySet()) {
            checkAnnotation(shardAnnotationsPrimaryService, entry.getKey());
          }
          for (var entry : shardsAnnotations.getClusterPods().entrySet()) {
            checkAnnotation(shardAnnotationsClusterPods, entry.getKey());
          }
          for (var entry : shardsAnnotations.getAllResources().entrySet()) {
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
