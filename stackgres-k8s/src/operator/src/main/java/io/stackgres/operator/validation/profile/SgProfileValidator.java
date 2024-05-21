/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

interface SgProfileValidator extends Validator<StackGresInstanceProfileReview> {
}
