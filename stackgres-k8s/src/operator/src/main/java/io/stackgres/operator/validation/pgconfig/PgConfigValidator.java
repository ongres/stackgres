/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface PgConfigValidator extends Validator<StackGresPostgresConfigReview> {

}
