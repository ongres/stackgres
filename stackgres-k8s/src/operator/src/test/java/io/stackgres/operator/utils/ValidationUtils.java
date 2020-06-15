/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresContext;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtils {

  public static String getRandomString(int length) {
    byte[] array = new byte[length];
    ThreadLocalRandom.current().nextBytes(array);
    return new String(array, StandardCharsets.UTF_8);
  }

  public static void assertValidationFailed(Executable executable, String message, Integer code) {
    ValidationFailed validation = assertThrows(ValidationFailed.class, executable);
    assertEquals(message, validation.getResult().getMessage());
    assertEquals(code, validation.getResult().getCode());
  }

  public static void assertValidationFailed(Executable executable, String message) {
    assertValidationFailed(executable, message, 400);
  }

  public static void assertErrorType(ValidationFailed ex, ErrorType errorType) {
    String errorTypeDocumentationUri = ValidationUtils
        .generateErrorTypeDocumentationUri(errorType);

    assertEquals(errorTypeDocumentationUri, ex.getResult().getReason(),
        "Error type didn't match. Status message: " + ex.getResult().getMessage());
  }

  public static ValidationFailed assertErrorType(ErrorType errorType, Executable executable) {

    ValidationFailed validation = assertThrows(ValidationFailed.class, executable);

    assertErrorType(validation, errorType);

    return validation;

  }

  public static void checkErrorCause(Status status, String field, String message, String reason) {
    final StatusDetails details = status.getDetails();
    assertNotNull(details);
    if (details.getCauses().size() == 1) {
      assertEquals(field, details.getName());
    }
    StatusCause cause = details.getCauses().stream()
        .filter(statusCause -> statusCause.getField().equals(field))
        .findFirst().orElseThrow(() -> new AssertionFailedError("Not cause with the field "
            + field + " was found"));
    assertEquals(message, cause.getMessage());
    assertEquals(reason, cause.getReason());
  }

  public static String generateErrorTypeDocumentationUri(ErrorType constraintViolation) {
    String documentationUri = StackGresContext.DOCUMENTATION_URI;
    String operatorVersion = StackGresContext.OPERATOR_VERSION;
    String errorsPath = StackGresContext.DOCUMENTATION_ERRORS_PATH;

    return String
        .format("%s%s%s%s",
            documentationUri,
            operatorVersion,
            errorsPath,
            constraintViolation.getUri());
  }

  public static String getNotNullMessage(Class<?> from, String field) {

    return getConstraintMessage(from, field, NotNull.class);

  }

  public static String getPositiveMessage(Class<?> from, String field) {

    return getConstraintMessage(from, field, Positive.class);

  }

  public static String getPatternMessage(Class<?> from, String field) {

    return getConstraintMessage(from, field, Pattern.class);

  }

  public static String getNotEmptyMessage(Class<?> from, String field) {

    return getConstraintMessage(from, field, NotEmpty.class);

  }

  public static String getConstraintMessage(Class<?> from, String field, Class<? extends Annotation> constraint) {
    try {
      final Annotation annotation = from.getDeclaredField(field).getAnnotation(constraint);

      Method messageMethod = annotation.getClass().getDeclaredMethod("message");

      return (String) messageMethod.invoke(annotation);
    } catch (NoSuchFieldException e) {
      throw new AssertionFailedError(field + " field not found", e);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new AssertionFailedError(constraint.getName() + " is not valid constraint annotation", e);
    }
  }
}
