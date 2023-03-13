/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public interface Validator<T extends AdmissionReview<?>> {

  void validate(T review) throws ValidationFailed;

  default String escapeFieldName(String name) {
    return name.replace(".", "\\.").replace("[", "\\[");
  }

  @SuppressWarnings("unchecked")
  default String getFieldPath(
      Class<?> clazz1, String field1) {
    return getFieldPath(
        Tuple.tuple(clazz1, field1));
  }

  @SuppressWarnings("unchecked")
  default String getFieldPath(
      Class<?> clazz1, String field1,
      Class<?> clazz2, String field2) {
    return getFieldPath(
        Tuple.tuple(clazz1, field1),
        Tuple.tuple(clazz2, field2));
  }

  @SuppressWarnings("unchecked")
  default String getFieldPath(
      Class<?> clazz1, String field1,
      Class<?> clazz2, String field2,
      Class<?> clazz3, String field3) {
    return getFieldPath(
        Tuple.tuple(clazz1, field1),
        Tuple.tuple(clazz2, field2),
        Tuple.tuple(clazz3, field3));
  }

  @SuppressWarnings("unchecked")
  default String getFieldPath(
      Tuple2<Class<?>, String>...fieldAndClazzList) {
    return Arrays.asList(fieldAndClazzList)
        .stream()
        .map(Unchecked.function(fieldAndClazz -> Optional.ofNullable(
            fieldAndClazz.v1.getDeclaredField(fieldAndClazz.v2)
            .getAnnotation(JsonProperty.class))
            .map(JsonProperty::value)
            .or(() -> Optional.of(fieldAndClazz.v2))
            .map(this::escapeFieldName)
            .orElseThrow()))
        .collect(Collectors.joining("."));
  }

  /**
   * Check value exists and is not empty.
   */
  default void checkIfProvided(String value, @NotNull String field, String...messageSuffixes)
      throws ValidationFailed {
    if (value == null || value.isEmpty()) {
      throw new ValidationFailed(field + " must be provided"
          + (messageSuffixes.length == 0 ? ""
              : " " + Arrays.asList(messageSuffixes).stream().collect(Collectors.joining(" "))));
    }
  }

  default void fail(String kind, String reason, String message) throws ValidationFailed {
    Status status = new StatusBuilder()
        .withMessage(message)
        .withKind(kind)
        .withCode(400)
        .withReason(reason)
        .build();
    throw new ValidationFailed(status);
  }

  default void failWithFields(String kind, String reason, String message, String... fields)
      throws ValidationFailed {
    StatusDetailsBuilder statusDetailsBuilder = new StatusDetailsBuilder();
    Arrays.asList(fields).forEach(field -> statusDetailsBuilder
        .addNewCause(field, message, reason));
    Status status = new StatusBuilder()
        .withMessage(message)
        .withKind(kind)
        .withCode(400)
        .withReason(reason)
        .withDetails(statusDetailsBuilder.build())
        .build();
    throw new ValidationFailed(status);
  }

}
