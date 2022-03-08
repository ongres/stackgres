/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.Status;
import io.stackgres.common.ErrorType;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ConstraintValidationTest<T extends AdmissionReview<?>> {

  protected ConstraintValidator<T> validator;

  private String errorTypeDocumentationUri;

  @BeforeEach
  void setUp() {
    validator = buildValidator();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator.setConstraintValidator(factory.getValidator());
    validator.init();

    this.errorTypeDocumentationUri = ErrorType.getErrorTypeUri(
        ErrorType.CONSTRAINT_VIOLATION);

  }

  @Test
  void validResource_shouldNotThrowAnyValidationError() throws ValidationFailed {

    T validReview = getValidReview();

    validator.validate(validReview);

  }

  @Test
  void ifReviewIsNull_shouldPass() throws ValidationFailed {

    T validReview = getValidReview();
    validReview.getRequest().setObject(null);

    validator.validate(validReview);
  }

  @Test
  void validationErrors_shouldIncludeTheErrorType() {

    T invalidReview = getInvalidReview();

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(invalidReview);
    });

    assertNotNull(ex.getResult(), "constraint violations should return a result");

    Status status = ex.getResult();

    assertNotNull(status.getReason(), "constraint violations should have a reason");

    assertEquals(errorTypeDocumentationUri, status.getReason());

  }

  protected abstract ConstraintValidator<T> buildValidator();

  protected abstract T getValidReview();

  protected abstract T getInvalidReview();

  protected void checkNotNullErrorCause(Class<?> outerClass, String fieldPath, T review) {

    String lastField = getLastField(fieldPath);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    String message = ValidationUtils.getNotNullMessage(outerClass, lastField);

    ValidationUtils.checkErrorCause(ex.getResult(), fieldPath, message,
        NotNull.class.getName());

  }

  protected void checkNotEmptyErrorCause(Class<?> outerClass, String fieldPath, T review) {

    String lastField = getLastField(fieldPath);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    String message = ValidationUtils.getNotEmptyMessage(outerClass, lastField);

    ValidationUtils.checkErrorCause(ex.getResult(), fieldPath, message,
        NotEmpty.class.getName());

  }

  protected void checkErrorCause(Class<?> outerClass, String fieldPath, T review,
      Class<? extends Annotation> constraint) {
    checkErrorCause(outerClass, fieldPath, fieldPath, review, constraint, null);
  }

  protected void checkErrorCause(Class<?> outerClass, String fieldPath, T review,
      Class<? extends Annotation> constraint, String message) {
    checkErrorCause(outerClass, fieldPath, fieldPath, review, constraint, message);
  }

  protected void checkErrorCause(Class<?> outerClass, String fieldPath, String validationPath,
      T review, Class<? extends Annotation> constraint) {
    checkErrorCause(outerClass, new String[] {fieldPath}, validationPath, review, constraint, null);
  }

  protected void checkErrorCause(Class<?> outerClass, String fieldPath, String validationPath,
      T review, Class<? extends Annotation> constraint, String message) {
    checkErrorCause(outerClass, new String[] {fieldPath},
        validationPath, review, constraint, message);
  }

  protected void checkErrorCause(Class<?> outerClass, String[] fieldPaths, String validationPath,
      T review, Class<? extends Annotation> constraint) {
    checkErrorCause(outerClass, fieldPaths, validationPath, review, constraint, null);
  }

  protected void checkErrorCause(Class<?> outerClass, String[] fieldPaths, String validationPath,
      T review, Class<? extends Annotation> constraint, String message) {

    String lastField = getLastField(validationPath);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    if (message == null) {
      message = ValidationUtils.getConstraintMessage(outerClass, lastField, constraint);
    }

    ValidationUtils.checkErrorCause(ex.getResult(), fieldPaths, message,
        constraint.getName());
  }

  private static String getLastField(String fieldPath) {
    final String[] split = fieldPath.split("\\.");
    return split[split.length - 1];
  }
}
