/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class SgProfileDependenciesValidator extends DependenciesValidator<SgProfileReview>
    implements SgProfileValidator {

  @Override
  protected void validate(SgProfileReview review, StackGresCluster i) throws ValidationFailed {
    if (i.getSpec().getResourceProfile().equals(review.getRequest().getName())) {
      fail(review, i);
    }
  }
}
