/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsDefinition;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface DbOpsValidator extends Validator<StackGresDbOpsReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(StackGresDbOpsDefinition.KIND, reason, message);
  }

  default void fail(String message) throws ValidationFailed {
    ValidationType validationType = this.getClass().getAnnotation(ValidationType.class);
    String errorTypeUri = ErrorType.getErrorTypeUri(validationType.value());
    fail(errorTypeUri, message);
  }

}
