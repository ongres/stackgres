/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import io.stackgres.common.ConfigContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsDefinition;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface DistributedLogsValidator extends Validator<StackGresDistributedLogsReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(StackGresDistributedLogsDefinition.KIND, reason, message);
  }

  default void fail(String message) throws ValidationFailed {
    ValidationType validationType = this.getClass().getAnnotation(ValidationType.class);
    String errorTypeUri = ConfigContext.getErrorTypeUri(validationType.value());
    fail(errorTypeUri, message);
  }

}
