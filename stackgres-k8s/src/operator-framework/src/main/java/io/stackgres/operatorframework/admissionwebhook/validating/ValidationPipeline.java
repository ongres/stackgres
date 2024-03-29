/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import java.util.List;

import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import org.jetbrains.annotations.NotNull;

public abstract class ValidationPipeline<T extends AdmissionReview<?>> {

  private final List<Validator<T>> validators;

  protected ValidationPipeline(List<Validator<T>> validators) {
    this.validators = validators;
  }

  public void validate(@NotNull T review) throws ValidationFailed {
    for (var validator : this.validators) {
      validator.validate(review);
    }
  }

}
