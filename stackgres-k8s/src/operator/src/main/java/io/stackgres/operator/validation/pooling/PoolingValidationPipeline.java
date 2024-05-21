/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class PoolingValidationPipeline extends AbstractValidationPipeline<StackGresPoolingConfigReview> {

  @Inject
  public PoolingValidationPipeline(
      @Any Instance<Validator<StackGresPoolingConfigReview>> validatorInstances) {
    super(validatorInstances);
  }

}
