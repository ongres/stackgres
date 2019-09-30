/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.validators;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.validation.AdmissionReview;

@ApplicationScoped
public class ValidationPipeline {

  private Instance<ClusterValidator> validators;

  @Inject
  public ValidationPipeline(Instance<ClusterValidator> validators) {
    this.validators = validators;
  }

  public void validator(AdmissionReview admissionReview) throws ValidationFailed {

    for (ClusterValidator validator: validators) {

      validator.validate(admissionReview);

    }

  }

}
