/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class DbOpsValidationPipeline implements ValidationPipeline<DbOpsReview> {

  private SimpleValidationPipeline<DbOpsReview, DbOpsValidator> genericPipeline;

  /**
   * Validate all {@code Validator}s in sequence.
   */
  @Override
  public void validate(DbOpsReview review) throws ValidationFailed {
    genericPipeline.validate(review);

  }

  @Inject
  public void setValidators(@Any Instance<DbOpsValidator> validators) {
    this.genericPipeline = new SimpleValidationPipeline<>(validators);
  }

}
