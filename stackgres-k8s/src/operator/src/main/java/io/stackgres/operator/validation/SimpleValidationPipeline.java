/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.enterprise.inject.Instance;

import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public class SimpleValidationPipeline<T extends AdmissionReview<?>, V extends Validator<T>>
    implements io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline<T> {

  private List<V> validators;

  public SimpleValidationPipeline(Instance<V> validatorInstances) {
    init(validatorInstances);
  }

  private Comparator<? super V> validationTypeComparator() {
    return (v1, v2) -> {
      final ValidationType v1ValidationType = v1.getClass().getAnnotation(ValidationType.class);
      final ValidationType v2ValidationType = v2.getClass().getAnnotation(ValidationType.class);

      if (v1ValidationType == null && v2ValidationType == null) {
        return 0;
      } else if (v1ValidationType == null) {
        return -1;
      } else if (v2ValidationType == null) {
        return 1;
      } else {
        return v1ValidationType.value().compareTo(v2ValidationType.value());
      }
    };
  }

  private void init(Instance<V> validatorInstances) {
    this.validators = validatorInstances.stream()
        .sorted(validationTypeComparator()).toList();
  }

  @Override
  public void validate(T review) throws ValidationFailed {

    if (review.getRequest().getOperation() == Operation.UPDATE) {

      final Object object = review.getRequest().getObject();
      final Object oldObject = review.getRequest().getOldObject();

      if (Objects.equals(object, oldObject)) {
        return;
      }
    }

    for (V validator : validators) {
      validator.validate(review);
    }
  }

}
