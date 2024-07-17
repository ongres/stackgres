/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import java.util.Objects;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class PoolingDependenciesValidator
    extends DependenciesValidator<StackGresPoolingConfigReview, StackGresCluster>
    implements PoolingValidator {

  @Override
  public void validate(StackGresPoolingConfigReview review, StackGresCluster resource)
      throws ValidationFailed {
    if (Objects.equals(resource.getSpec().getConfigurations().getSgPoolingConfig(),
        review.getRequest().getName())) {
      fail(review, resource);
    }
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

}
