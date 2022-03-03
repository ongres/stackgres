/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class AbstractDefaultConfigKeeper
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements Validator<T> {

  private final String errorTypeUri =
      ErrorType.getErrorTypeUri(ErrorType.DEFAULT_CONFIGURATION);

  private DefaultCustomResourceHolder<R> defaultCustomResourceHolder;

  @Override
  public void validate(T review) throws ValidationFailed {
    final AdmissionRequest<R> request = review.getRequest();
    switch (request.getOperation()) {
      case UPDATE -> {
        final R object = request.getObject();
        String updateName = object.getMetadata().getName();
        if (defaultCustomResourceHolder.isDefaultCustomResource(object)) {
          final String message = "Cannot update CR " + updateName + " because is a default CR";
          fail(request.getKind().getKind(), errorTypeUri, message);
        }
      }
      case DELETE -> {
        String deleteNamespace = request.getNamespace();
        String deleteName = request.getName();
        if (defaultCustomResourceHolder.isDefaultCustomResource(deleteName, deleteNamespace)) {
          final String message = "Cannot delete CR " + deleteName + " because is a default CR";
          fail(request.getKind().getKind(), errorTypeUri, message);
        }
      }
      default -> {
      }
    }
  }

  @Inject
  public void setDefaultCustomResourceHolder(
      DefaultCustomResourceHolder<R> defaultCustomResourceHolder) {
    this.defaultCustomResourceHolder = defaultCustomResourceHolder;
  }
}
