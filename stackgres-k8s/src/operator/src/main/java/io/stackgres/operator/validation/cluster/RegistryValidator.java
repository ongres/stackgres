/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class RegistryValidator implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case UPDATE:
        if (StackGresUtil.isRegistryEnabled(review.getRequest().getObject())
            != StackGresUtil.isRegistryEnabled(review.getRequest().getOldObject())) {
          fail("registry enabled can not be changed");
        }
        break;
      default:
    }
  }

}
