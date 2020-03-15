/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operator.validation.ValidationType;

@ApplicationScoped
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class PgBouncerConstraintValidator extends ConstraintValidator<PgBouncerReview>
    implements PgBouncerValidator {
}
