/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class ObjectStorageForbiddenValidatorTest {

  private ObjectStorageForbiddenValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ObjectStorageForbiddenValidator();
  }

  @Test
  public void givenAnyCreation_shouldFail()
      throws ValidationFailed {
    final ObjectStorageReview review = getEmptyReview();

    ValidationFailed ex = ValidationUtils.assertErrorType(ErrorType.FORBIDDEN_CR_CREATION,
        () -> validator.validate(review));

    assertEquals("This version of StackGres does not support StackGresObjectStorage CRD",
        ex.getResult().getMessage());
  }

  private ObjectStorageReview getEmptyReview() {
    ObjectStorageReview review = JsonUtil
        .readFromJson("objectstorage_allow_request/create.json", ObjectStorageReview.class);
    return review;
  }

}
