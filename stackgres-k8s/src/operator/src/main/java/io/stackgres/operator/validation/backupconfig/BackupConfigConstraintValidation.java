/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operator.validation.ValidationType;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class BackupConfigConstraintValidation extends ConstraintValidator<BackupConfigReview>
    implements BackupConfigValidator {

}
