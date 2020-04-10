/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class PgConfigDependenciesValidator extends DependenciesValidator<PgConfigReview>
    implements PgConfigValidator {

  @Override
  public void validate(PgConfigReview review, StackGresCluster i) throws ValidationFailed {
    if (review.getRequest().getName().equals(i.getSpec().getConfiguration().getPostgresConfig())) {
      fail(review, i);
    }
  }

}
