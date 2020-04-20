/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterDefinition;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface ClusterValidator extends Validator<StackGresClusterReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(StackGresClusterDefinition.KIND, reason, message);
  }

  default void fail(ConfigContext context, String message) throws ValidationFailed {
    ValidationType validationType = this.getClass().getAnnotation(ValidationType.class);
    String errorTypeUri = context.getErrorTypeUri(validationType.value());
    fail(errorTypeUri, message);
  }

}
