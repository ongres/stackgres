/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import java.util.Objects;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class SgProfileDependenciesValidator
    extends DependenciesValidator<StackGresInstanceProfileReview, StackGresCluster>
    implements SgProfileValidator {

  @Override
  protected void validate(StackGresInstanceProfileReview review, StackGresCluster resource)
      throws ValidationFailed {
    if (Objects.equals(resource.getSpec().getSgInstanceProfile(), review.getRequest().getName())) {
      fail(review, resource);
    }
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

}
