/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.CaseFormat;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.stackgres.common.ClassUtil;
import io.stackgres.common.ErrorType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

public class ValidationUtils {

  public static void assertValidationFailed(Executable executable, String message, Integer code) {
    ValidationFailed validation = assertThrows(ValidationFailed.class, executable);
    assertEquals(message, validation.getResult().getMessage());
    assertEquals(code, validation.getResult().getCode());
  }

  public static void assertValidationFailed(Executable executable, String message) {
    assertValidationFailed(executable, message, 400);
  }

  public static void assertValidationFailed(Executable executable, ErrorType errorType,
      String message) {
    assertValidationFailed(executable, errorType, message, new String[0]);
  }

  public static void assertValidationFailed(Executable executable, ErrorType errorType,
      String message, String...fields) {
    ValidationFailed validation = assertThrows(ValidationFailed.class, executable);
    assertEquals(message, validation.getResult().getMessage());
    assertEquals(400, validation.getResult().getCode());
    String errorTypeDocumentationUri = ErrorType.getErrorTypeUri(errorType);
    Arrays.asList(fields).forEach(field -> assertTrue(
        validation.getResult().getDetails().getCauses().stream().anyMatch(
            cause -> Objects.equals(field, cause.getField())
            && Objects.equals(message, cause.getMessage())
            && Objects.equals(errorTypeDocumentationUri, cause.getReason()))));

    assertErrorType(validation, errorType);
  }

  public static void assertErrorType(ValidationFailed ex, ErrorType errorType) {
    String errorTypeDocumentationUri = ErrorType.getErrorTypeUri(errorType);

    assertEquals(errorTypeDocumentationUri, ex.getResult().getReason(),
        "Error type didn't match. Status message: " + ex.getResult().getReason());
  }

  public static ValidationFailed assertErrorType(ErrorType errorType, Executable executable) {

    ValidationFailed validation = assertThrows(ValidationFailed.class, executable);

    assertErrorType(validation, errorType);

    return validation;

  }

  public static void checkErrorCause(Status status, String field, String message, String reason) {
    checkErrorCause(status, new String[] {field}, message, reason);
  }

  public static void checkErrorCause(Status status, String[] fields, String message,
      String reason) {
    final StatusDetails details = status.getDetails();
    assertNotNull(details);
    assertEquals(fields.length,
        details.getCauses().stream()
        .filter(cause -> cause.getMessage().equals(message))
        .count(),
        "details was:\n"
            + details.getCauses().stream()
            .filter(cause -> cause.getMessage().equals(message))
            .map(cause -> cause.getField() + ": " + cause.getMessage())
            .collect(Collectors.joining("\n")));
    if (details.getCauses().size() == 1) {
      assertEquals(fields[0], details.getName());
    }
    Seq.of(fields).forEach(field -> {
      StatusCause cause = details.getCauses().stream()
          .filter(statusCause -> statusCause.getField().equals(field))
          .findFirst().orElseThrow(() -> new AssertionFailedError("Not cause with the field "
              + field + " was found"));
      assertEquals(message, cause.getMessage());
      assertEquals(reason, cause.getReason());
    });
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

  public static String getConstraintMessage(Class<?> from, String propertyOrFieldOrMethod,
      Class<? extends Annotation> constraint) {
    try {
      Annotation annotation;
      AssertionFailedError ex = new AssertionFailedError(
          constraint.getName() + " for property / field / method " + propertyOrFieldOrMethod
              + " is not valid constraint annotation");
      final String fieldOrMethod = ClassUtil.getDeclaredFieldsFromClassHierarchy(from)
          .map(clazzField -> Optional.ofNullable(clazzField.getAnnotation(JsonProperty.class))
              .map(jsonProperty -> Tuple.tuple(clazzField.getName(), jsonProperty.value())))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(tuple -> tuple.v2.equals(propertyOrFieldOrMethod))
          .map(Tuple2::v1)
          .findFirst()
          .orElse(propertyOrFieldOrMethod);
      while (true) {
        try {
          annotation = from.getDeclaredField(fieldOrMethod).getAnnotation(constraint);
          break;
        } catch (NoSuchFieldException e) {
          ex.addSuppressed(e);
        }
        try {
          annotation = from.getDeclaredMethod(fieldOrMethod).getAnnotation(constraint);
          break;
        } catch (NoSuchMethodException e) {
          ex.addSuppressed(e);
        }
        try {
          annotation =
              from.getDeclaredMethod(fieldToGetter(fieldOrMethod)).getAnnotation(constraint);
          break;
        } catch (NoSuchMethodException e) {
          ex.addSuppressed(e);
        }
        from = from.getSuperclass();
        if (from == Object.class) {
          throw ex;
        }
      }

      Method messageMethod = annotation.getClass().getDeclaredMethod("message");

      return (String) messageMethod.invoke(annotation);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new AssertionFailedError(constraint.getName() + " is not valid constraint annotation",
          e);
    }
  }

  private static String fieldToGetter(String name) {
    return "get" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
  }

}
