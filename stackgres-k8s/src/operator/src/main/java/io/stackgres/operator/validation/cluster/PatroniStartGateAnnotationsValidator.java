/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class PatroniStartGateAnnotationsValidator implements ClusterValidator {

  private final String startGateAnnotationsPath;

  public PatroniStartGateAnnotationsValidator() {
    this.startGateAnnotationsPath = getFieldPath(
        StackGresCluster.class, "spec",
        StackGresClusterSpec.class, "configurations",
        StackGresClusterConfigurations.class, "patroni",
        StackGresClusterPatroni.class, "startGateAnnotations");
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return;
    }
    final Map<String, String> startGateAnnotations =
        Optional.of(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getStartGateAnnotations)
        .orElse(Map.of());
    for (var startGateAnnotation : startGateAnnotations.entrySet()) {
      try {
        ResourceUtil.annotationKeySyntax(startGateAnnotation.getKey());
      } catch (IllegalArgumentException ex) {
        failWithMessageAndFields(
            HasMetadata.getKind(StackGresCluster.class),
            ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION),
            ex.getMessage(),
            String.format("%s.%s", startGateAnnotationsPath, startGateAnnotation.getKey()),
            startGateAnnotationsPath);
      }
    }
  }

}
