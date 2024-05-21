/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import java.util.Objects;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class PgConfigLogsDependenciesValidator
    extends DependenciesValidator<StackGresPostgresConfigReview, StackGresDistributedLogs>
    implements PgConfigValidator {

  @Override
  public void validate(StackGresPostgresConfigReview review, StackGresDistributedLogs resource)
      throws ValidationFailed {
    if (Objects.equals(resource.getSpec().getConfigurations().getSgPostgresConfig(),
        review.getRequest().getName())) {
      fail(review, resource);
    }
  }

  @Override
  protected Class<StackGresDistributedLogs> getResourceClass() {
    return StackGresDistributedLogs.class;
  }

}
