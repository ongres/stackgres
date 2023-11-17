/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultPoolingConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresPoolingConfig, PoolingReview>
    implements PoolingValidator {

}
