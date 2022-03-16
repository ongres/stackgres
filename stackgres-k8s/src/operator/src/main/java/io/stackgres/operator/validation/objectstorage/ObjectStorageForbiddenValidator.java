/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_CREATION)
public class ObjectStorageForbiddenValidator implements ObjectStorageValidator {

  private final String errorTypeUri = ErrorType
      .getErrorTypeUri(ErrorType.FORBIDDEN_CR_CREATION);

  @Override
  public void validate(ObjectStorageReview review) throws ValidationFailed {

    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE) {
      fail("This version of StackGres does not support StackGresObjectStorage CRD");
    }
  }

  public void fail(String message) throws ValidationFailed {
    fail(errorTypeUri, message);
  }
}
