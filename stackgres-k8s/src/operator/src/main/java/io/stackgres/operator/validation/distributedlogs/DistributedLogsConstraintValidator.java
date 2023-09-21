/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ValidationType;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class DistributedLogsConstraintValidator
    extends AbstractConstraintValidator<StackGresDistributedLogsReview>
    implements DistributedLogsValidator {
}
