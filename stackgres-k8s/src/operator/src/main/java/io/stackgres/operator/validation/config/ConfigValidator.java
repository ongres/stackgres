/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.config;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface ConfigValidator extends Validator<ConfigReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(HasMetadata.getKind(StackGresConfig.class), reason, message);
  }

}
