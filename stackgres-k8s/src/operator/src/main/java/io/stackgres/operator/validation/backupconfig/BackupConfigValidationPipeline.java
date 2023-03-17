/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.AbstractValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

@ApplicationScoped
public class BackupConfigValidationPipeline extends AbstractValidationPipeline<BackupConfigReview> {

  @Inject
  public BackupConfigValidationPipeline(
      @Any Instance<Validator<BackupConfigReview>> validatorInstances) {
    super(validatorInstances);
  }

}
