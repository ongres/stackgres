/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operatorframework.Validator;

public interface RestoreConfigValidator extends Validator<RestoreConfigReview> {
}
