/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractAnnotationMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements Mutator<R, T> {

  @Override
  public R mutate(T review, R resource) {
    AdmissionRequest<R> request = review.getRequest();
    if (request.getOperation() == Operation.CREATE) {
      setDefaultAnnotations(resource);
    } else if (request.getOperation() == Operation.UPDATE
        && !getAnnotationsToOverwrite(resource).isEmpty()) {
      setDefaultAnnotations(resource);
    }
    return resource;
  }

  private void setDefaultAnnotations(R resource) {
    Map<String, String> defaultAnnotations = getDefaultAnnotationValues();
    Map<String, String> annotationsToOverwrite = getAnnotationsToOverwrite(resource);
    Map<String, String> existingAnnotations =
        Optional.ofNullable(resource.getMetadata().getAnnotations())
        .orElse(Map.of());
    resource.getMetadata().setAnnotations(
        Seq.seq(existingAnnotations)
        .append(Seq.seq(defaultAnnotations)
            .filter(defaultAnnotation -> Seq.seq(existingAnnotations)
                .map(Tuple2::v1)
                .noneMatch(defaultAnnotation.v1::equals)))
        .filter(annotation -> Seq.seq(annotationsToOverwrite)
            .map(Tuple2::v1)
            .noneMatch(annotation.v1::equals))
        .append(Seq.seq(annotationsToOverwrite))
        .toMap(Tuple2::v1, Tuple2::v2));
  }

  private Map<String, String> getDefaultAnnotationValues() {
    String operatorVersion = StackGresProperty.OPERATOR_VERSION.getString();
    Objects.requireNonNull(operatorVersion, "stackgres.operatorVersion must not be null");
    if (operatorVersion.isBlank()) {
      throw new IllegalArgumentException("stackgres.operatorVersion must not be empty");
    }

    String operatorVersionKey = StackGresContext.VERSION_KEY;

    return Map.of(operatorVersionKey, operatorVersion);
  }

  protected Map<String, String> getAnnotationsToOverwrite(R resource) {
    return Map.of();
  }
}
