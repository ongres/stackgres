/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

@ApplicationScoped
public class BackupValidationPipeline extends AbstractValidationPipeline<BackupReview> {

  @Inject
  public BackupValidationPipeline(
      @Any Instance<Validator<BackupReview>> validatorInstances) {
    super(validatorInstances);
  }

}
