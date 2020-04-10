/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class DependenciesValidator<T extends AdmissionReview<?>> implements Validator<T> {

  private CustomResourceScanner<StackGresCluster> clusterScanner;

  private String errorTypeUri;

  @Inject
  public void init(ConfigContext configContext) {
    errorTypeUri = configContext.getErrorTypeUri(ErrorType.FORBIDDEN_CR_DELETION);
  }

  @Override
  public void validate(T review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.DELETE
        && review.getRequest().getName() != null) {
      Optional<List<StackGresCluster>> clusters = clusterScanner
          .findResources(review.getRequest().getNamespace());

      if (clusters.isPresent()) {
        for (StackGresCluster i : clusters.get()) {
          validate(review, i);
        }
      }
    }
  }

  protected abstract void validate(T review, StackGresCluster i) throws ValidationFailed;

  protected void fail(T review, StackGresCluster i) throws ValidationFailed {
    final AdmissionRequest<?> request = review.getRequest();
    final String message = "Can't " + request.getOperation().name().toLowerCase()
        + " " + request.getResource().getResource()
        + "." + request.getKind().getGroup()
        + " " + request.getName() + " because the "
        + StackGresClusterDefinition.NAME + " " + i.getMetadata().getName() + " depends on it";

    Status status = new StatusBuilder()
        .withCode(409)
        .withKind(request.getKind().getKind())
        .withReason(errorTypeUri)
        .withMessage(message)
        .build();
    throw new ValidationFailed(status);
  }

  @Inject
  public void setClusterScanner(CustomResourceScanner<StackGresCluster> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

}
