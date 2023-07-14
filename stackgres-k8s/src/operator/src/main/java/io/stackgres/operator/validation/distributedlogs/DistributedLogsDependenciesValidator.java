/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class DistributedLogsDependenciesValidator
    extends DependenciesValidator<StackGresDistributedLogsReview, StackGresCluster>
    implements DistributedLogsValidator {

  @Override
  public void validate(StackGresDistributedLogsReview review, StackGresCluster resource)
      throws ValidationFailed {
    if (Optional.ofNullable(resource.getSpec().getDistributedLogs())
        .map(distributedLogs -> review.getRequest().getName().equals(
            distributedLogs.getDistributedLogs()))
        .orElse(false)) {
      fail(review, resource);
    }
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

}
