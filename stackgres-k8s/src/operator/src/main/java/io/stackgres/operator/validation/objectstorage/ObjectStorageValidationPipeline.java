/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class ObjectStorageValidationPipeline implements ValidationPipeline<ObjectStorageReview> {

  private SimpleValidationPipeline<ObjectStorageReview, ObjectStorageValidator> pipeline;

  @Override
  public void validate(ObjectStorageReview review) throws ValidationFailed {
    pipeline.validate(review);
  }

  @Inject
  public void setValidators(@Any Instance<ObjectStorageValidator> validators) {
    this.pipeline = new SimpleValidationPipeline<>(validators);
  }
}
