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
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class DistributedLogsValidationPipeline
    implements ValidationPipeline<StackGresDistributedLogsReview> {

  private SimpleValidationPipeline<StackGresDistributedLogsReview, DistributedLogsValidator>
      genericPipeline;

  /**
   * Validate all {@code Validator}s in sequence.
   */
  @Override
  public void validate(StackGresDistributedLogsReview review) throws ValidationFailed {
    genericPipeline.validate(review);
  }

  @Inject
  public void setValidators(@Any Instance<DistributedLogsValidator> validators) {
    this.genericPipeline = new SimpleValidationPipeline<>(validators);
  }

}
