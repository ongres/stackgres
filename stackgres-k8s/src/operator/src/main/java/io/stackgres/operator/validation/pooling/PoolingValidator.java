/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface PoolingValidator extends Validator<StackGresPoolingConfigReview> {
}
