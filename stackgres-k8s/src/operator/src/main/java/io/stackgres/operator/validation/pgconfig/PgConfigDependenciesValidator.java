/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Objects;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class PgConfigDependenciesValidator
    extends DependenciesValidator<PgConfigReview, StackGresCluster>
    implements PgConfigValidator {

  @Override
  public void validate(PgConfigReview review, StackGresCluster resource)
      throws ValidationFailed {
    if (Objects.equals(resource.getSpec().getConfigurations().getSgPostgresConfig(),
        review.getRequest().getName())) {
      fail(review, resource);
    }
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

}
