/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.validators;

import io.stackgres.operator.validation.AdmissionReview;

public interface Validator {

  void validate(AdmissionReview review) throws ValidationFailed;

}
