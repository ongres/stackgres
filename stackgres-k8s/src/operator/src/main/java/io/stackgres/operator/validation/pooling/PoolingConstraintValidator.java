/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operator.validation.ValidationType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class PoolingConstraintValidator extends ConstraintValidator<PoolingReview>
    implements PoolingValidator {
}
