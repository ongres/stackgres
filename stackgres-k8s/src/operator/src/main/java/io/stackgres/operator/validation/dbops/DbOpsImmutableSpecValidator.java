/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import javax.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class DbOpsImmutableSpecValidator implements DbOpsValidator {

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
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
