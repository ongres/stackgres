/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamValidationPipeline extends AbstractValidationPipeline<StackGresStreamReview> {

  @Inject
  public StreamValidationPipeline(
      @Any Instance<Validator<StackGresStreamReview>> validatorInstances) {
    super(validatorInstances);
  }

}
