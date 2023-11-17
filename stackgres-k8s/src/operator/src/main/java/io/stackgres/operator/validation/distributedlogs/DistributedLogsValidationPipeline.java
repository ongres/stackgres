/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsValidationPipeline
    extends AbstractValidationPipeline<StackGresDistributedLogsReview> {

  @Inject
  public DistributedLogsValidationPipeline(
      @Any Instance<Validator<StackGresDistributedLogsReview>> validatorInstances) {
    super(validatorInstances);
  }

}
