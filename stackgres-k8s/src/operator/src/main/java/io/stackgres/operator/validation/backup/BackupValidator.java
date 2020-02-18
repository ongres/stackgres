/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backup;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface BackupValidator extends Validator<BackupReview> {
}
