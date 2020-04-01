/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.inject.Singleton;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class BackupConfigDependenciesValidator extends DependenciesValidator<BackupConfigReview>
    implements BackupConfigValidator {

  @Override
  public void validate(BackupConfigReview review, StackGresCluster i) throws ValidationFailed {
    if (review.getRequest().getName().equals(i.getSpec().getConfiguration().getBackupConfig())) {
      fail(review, i);
    }
  }

}
