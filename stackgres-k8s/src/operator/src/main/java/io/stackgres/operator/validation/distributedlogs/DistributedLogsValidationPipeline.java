/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

@ApplicationScoped
public class DistributedLogsValidationPipeline
    extends AbstractValidationPipeline<StackGresDistributedLogsReview> {

  @Inject
  public DistributedLogsValidationPipeline(
      @Any Instance<Validator<StackGresDistributedLogsReview>> validatorInstances) {
    super(validatorInstances);
  }

}
