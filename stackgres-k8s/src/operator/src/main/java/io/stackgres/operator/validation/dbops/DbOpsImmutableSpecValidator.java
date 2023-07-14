/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class DbOpsImmutableSpecValidator implements DbOpsValidator {

  @Override
  public void validate(DbOpsReview review) throws ValidationFailed {

    switch (review.getRequest().getOperation()) {
      case UPDATE:
        StackGresDbOps dbOps = review.getRequest().getObject();
        StackGresDbOps oldDbOps = review.getRequest().getOldObject();
        if (!dbOps.getSpec().equals(oldDbOps.getSpec())) {
          fail("spec can not be updated");
        }
        break;
      default:
    }

  }

}
