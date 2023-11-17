/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import jakarta.inject.Inject;

public abstract class DependenciesValidator<T extends AdmissionReview<?>,
        R extends CustomResource<?, ?>>
    implements Validator<T> {

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.FORBIDDEN_CR_DELETION);

  private CustomResourceScanner<R> resourceScanner;

  @Override
  public void validate(T review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.DELETE
        && review.getRequest().getName() != null) {
      Optional<List<R>> resources = resourceScanner
          .findResources(review.getRequest().getNamespace());

      if (resources.isPresent()) {
        for (R i : resources.get()) {
          validate(review, i);
        }
      }
    }
  }

  protected abstract void validate(T review, R resource) throws ValidationFailed;

  protected void fail(T review, R resource) throws ValidationFailed {
    final AdmissionRequest<?> request = review.getRequest();
    final String message = "Can't " + request.getOperation().toString()
        + " " + request.getResource().getResource()
        + "." + request.getKind().getGroup()
        + " " + request.getName() + " because the "
        + CustomResource.getCRDName(getResourceClass()) + " "
        + resource.getMetadata().getName() + " depends on it";

    Status status = new StatusBuilder()
        .withCode(409)
        .withKind(request.getKind().getKind())
        .withReason(errorTypeUri)
        .withMessage(message)
        .build();
    throw new ValidationFailed(status);
  }

  protected abstract Class<R> getResourceClass();

  @Inject
  public void setResourceScanner(CustomResourceScanner<R> resourceScanner) {
    this.resourceScanner = resourceScanner;
  }

}
