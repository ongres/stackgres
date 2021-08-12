/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import static io.stackgres.common.StackGresContext.APP_NAME;
import static io.stackgres.testutil.StringUtils.getOversizedResourceName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.Annotation;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Status;
import io.stackgres.common.ErrorType;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @Test
  void oversizedClusterName_shouldThrowValidationFailedException() throws ValidationFailed {

    T oversizedClusterNamespaceReview = getOversizedOperationNameReview();
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(oversizedClusterNamespaceReview);
    });

    assertNotNull(ex.getResult());
    assertEquals("Valid name or label must be 53 characters or less", ex.getMessage().toString());
  }

  @Test
  void oversizedLabel_shouldThrowValidationFailedException() throws ValidationFailed {

    T oversizedClusterNamespaceReview = getOversizedLabelReview();
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(oversizedClusterNamespaceReview);
    });

    assertNotNull(ex.getResult());
    assertEquals("Valid name or label must be 63 characters or less", ex.getMessage().toString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"stackgres.io/", "*9stackgres", "1143", "1143a", "-1143a", ".demo",
      "123-primary", "123-primary", "primary*", "stackgres-demo_1"})
  void invalidNames_shouldFail(String name) {
    T review = getValidReview();
    review.getRequest().getObject().getMetadata().setName(name);

    ValidationFailed message =
        assertThrows(ValidationFailed.class, () -> validator.validate(review));
    assertEquals("Name must consist of lower case alphanumeric "
        + "characters or '-', start with an alphabetic character, "
        + "and end with an alphanumeric character", message.getMessage());
  }

  protected T getOversizedOperationNameReview() {
    T oversizedClusterNamespaceReview = getValidReview();
    oversizedClusterNamespaceReview.getRequest().getObject().getMetadata()
        .setName(getOversizedResourceName());
    return oversizedClusterNamespaceReview;
  }

  protected T getOversizedLabelReview() {
    T oversizedClusterNamespaceReview = getValidReview();
    oversizedClusterNamespaceReview.getRequest().getObject().getMetadata()
        .setLabels(ImmutableMap.of(APP_NAME, getOversizedResourceName()));
    return oversizedClusterNamespaceReview;
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
    checkErrorCause(outerClass, fieldPath, fieldPath, review, constraint);
  }

  protected void checkErrorCause(Class<?> outerClass, String fieldPath, String validationPath,
      T review, Class<? extends Annotation> constraint) {
    checkErrorCause(outerClass, new String[] {fieldPath}, validationPath, review, constraint);
  }

  protected void checkErrorCause(Class<?> outerClass, String[] fieldPaths, String validationPath,
      T review, Class<? extends Annotation> constraint) {

    String lastField = getLastField(validationPath);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    String message = ValidationUtils.getConstraintMessage(outerClass, lastField, constraint);

    ValidationUtils.checkErrorCause(ex.getResult(), fieldPaths, message,
        constraint.getName());
  }

  private static String getLastField(String fieldPath) {
    final String[] split = fieldPath.split("\\.");
    return split[split.length - 1];
  }
}
