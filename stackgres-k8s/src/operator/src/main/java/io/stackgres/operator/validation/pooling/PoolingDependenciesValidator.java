/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@ApplicationScoped
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class PoolingDependenciesValidator extends DependenciesValidator<PoolingReview>
    implements PoolingValidator {

  @Override
  public void validate(PoolingReview review, StackGresCluster i) throws ValidationFailed {
    if (Objects.equals(i.getSpec().getConfiguration().getConnectionPoolingConfig(),
        review.getRequest().getName())) {
      fail(review, i);
    }
  }

}
