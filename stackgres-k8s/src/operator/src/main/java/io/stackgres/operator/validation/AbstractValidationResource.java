/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.stackgres.common.OperatorProperty;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationResource;

public class AbstractValidationResource<T extends AdmissionReview<?>>
    extends ValidationResource<T> {

  protected AbstractValidationResource(ValidationPipeline<T> pipeline) {
    super(OperatorProperty.getAllowedNamespaces(), pipeline);
  }

}
