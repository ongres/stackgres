/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ValidationType;

@ApplicationScoped
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class PoolingConstraintValidator extends AbstractConstraintValidator<PoolingReview>
    implements PoolingValidator {
}
