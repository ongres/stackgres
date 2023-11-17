/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ShardedDbOpsImmutableSpecValidator implements ShardedDbOpsValidator {

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(ShardedDbOpsReview review) throws ValidationFailed {

    switch (review.getRequest().getOperation()) {
      case UPDATE:
        StackGresShardedDbOps dbOps = review.getRequest().getObject();
        StackGresShardedDbOps oldDbOps = review.getRequest().getOldObject();
        if (!dbOps.getSpec().equals(oldDbOps.getSpec())) {
          fail("spec can not be updated");
        }
        break;
      default:
    }

  }

}
