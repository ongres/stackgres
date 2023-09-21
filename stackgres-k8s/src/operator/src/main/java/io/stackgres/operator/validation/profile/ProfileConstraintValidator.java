/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ValidationType;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ProfileConstraintValidator
    extends AbstractConstraintValidator<SgProfileReview>
    implements SgProfileValidator {
}
