/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class MetadataValidator implements DistributedLogsValidator {

  private final String annotationServicesPath;
  private final String annotationAllResourcesPath;
  private final String annotationClusterPodsPath;

  public MetadataValidator() {
    this.annotationServicesPath = getFieldPath(
      StackGresDistributedLogs.class, "spec",
      StackGresDistributedLogsSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "services"
    );

    this.annotationAllResourcesPath = getFieldPath(
      StackGresDistributedLogs.class, "spec",
      StackGresDistributedLogsSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "allResources"
    );

    this.annotationClusterPodsPath = getFieldPath(
      StackGresDistributedLogs.class, "spec",
      StackGresDistributedLogsSpec.class, "metadata",
      StackGresClusterSpecMetadata.class, "annotations",
      StackGresClusterSpecAnnotations.class, "clusterPods"
    );
  }

  @Override
  public void validate(StackGresDistributedLogsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case Operation.CREATE, Operation.UPDATE -> {

        final StackGresDistributedLogs cluster = review.getRequest().getObject();

        final Optional<StackGresClusterSpecAnnotations> maybeAnnotations = Optional
            .ofNullable(cluster.getSpec())
            .map(StackGresDistributedLogsSpec::getMetadata)
            .map(StackGresClusterSpecMetadata::getAnnotations);

        if (maybeAnnotations.isPresent()) {
          final StackGresClusterSpecAnnotations annotations = maybeAnnotations.get();

          final Map<String, String> services =
              Objects.requireNonNullElseGet(annotations.getServices(), Map::of);
          for (var entry : services.entrySet()) {
            checkAnnotation(annotationServicesPath, entry.getKey());
          }

          final Map<String, String> allResources =
              Objects.requireNonNullElseGet(annotations.getAllResources(), Map::of);
          for (var entry : allResources.entrySet()) {
            checkAnnotation(annotationAllResourcesPath, entry.getKey());
          }

          final Map<String, String> pods =
              Objects.requireNonNullElseGet(annotations.getClusterPods(), Map::of);
          for (var entry : pods.entrySet()) {
            checkAnnotation(annotationClusterPodsPath, entry.getKey());
          }
        }
      }

      default -> { }
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
