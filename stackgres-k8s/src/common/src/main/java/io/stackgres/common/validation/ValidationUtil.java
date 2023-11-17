/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.validation;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.stackgres.common.ClassUtil;
import jakarta.validation.ConstraintViolation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ValidationUtil {

  Logger LOGGER =
      LoggerFactory.getLogger(ValidationUtil.class);

  static List<String> getOffendingFields(
      final Object object, final ConstraintViolation<?> violation) {
    final String propertyPath = violation.getPropertyPath().toString();
    if (Seq.seq(violation.getConstraintDescriptor().getPayload())
        .map(fieldReference -> Optional.ofNullable(fieldReference
            .getAnnotation(FieldReference.ReferencedField.class)))
        .anyMatch(Optional::isPresent)) {
      final String basePath = propertyPath.substring(0, propertyPath.lastIndexOf('.'));
      return Seq.seq(violation.getConstraintDescriptor().getPayload())
          .map(fieldReference -> fieldReference.getAnnotation(FieldReference.ReferencedField.class))
          .map(FieldReference.ReferencedField::value)
          .map(field -> basePath + "." + field)
          .map(field -> replacePropertyPathWithJsonProperties(object, field))
          .toList();
    }
    return List.of(replacePropertyPathWithJsonProperties(object, propertyPath));
  }

  private static String replacePropertyPathWithJsonProperties(
      final Object object, final String propertyPath) {
    try {
      return Arrays.asList(propertyPath.split("\\.")).stream()
          .reduce(
              Tuple.<StringBuilder, Class<?>>tuple(new StringBuilder(), object.getClass()),
              ValidationUtil::replacePropertyPathWithJsonProperties,
              (u, v) -> v)
          .v1().toString();
    } catch (Exception ex) {
      LOGGER.warn("Can not translate path " + propertyPath + " using @JsonProperty annotations",
          ex);
      return propertyPath;
    }
  }

  private static Tuple2<StringBuilder, Class<?>> replacePropertyPathWithJsonProperties(
      Tuple2<StringBuilder, Class<?>> tuple, String field) {
    final int firstSquareIndex = field.indexOf('[');
    final String fieldName = firstSquareIndex < 0 ? field : field.substring(0, firstSquareIndex);
    final String fieldSuffix = firstSquareIndex < 0 ? "" : field.substring(firstSquareIndex);
    if (tuple.v2 == Object.class) {
      throw new IllegalArgumentException(
          "Field " + fieldName + " can not be found in class "
              + tuple.v2.getCanonicalName());
    }
    final String jsonFieldName = ClassUtil.getDeclaredFieldFromClassHierarchy(fieldName, tuple.v2)
        .map(clazzField -> clazzField.getAnnotation(JsonProperty.class))
        .map(JsonProperty::value)
        .map(ValidationUtil::escapeFieldName)
        .orElse(fieldName);
    return tuple
        .map1(builder -> builder.isEmpty()
            ? builder.append(jsonFieldName + fieldSuffix)
            : builder.append('.').append(jsonFieldName + fieldSuffix))
        .map2(clazz -> {
          if (clazz.isAssignableFrom(Map.class)) {
            return Class.class.cast(ParameterizedType.class
                .cast(clazz.getGenericSuperclass())
                .getActualTypeArguments()[1]);
          }
          if (firstSquareIndex < 0) {
            return ClassUtil.getDeclaredFieldFromClassHierarchy(fieldName, clazz)
                .<Class<?>>map(f -> f.getType())
                .orElse(Object.class);
          }
          return Class.class.cast(ParameterizedType.class
              .cast(ClassUtil.getDeclaredFieldFromClassHierarchy(fieldName, clazz)
                  .orElseThrow(() -> new IllegalArgumentException(
                      "Field " + fieldName + " can not be found in class "
                          + clazz.getCanonicalName()))
                  .getGenericType())
              .getActualTypeArguments()[0]);
        });
  }

  static String escapeFieldName(String name) {
    return name.replace(".", "\\.").replace("[", "\\[");
  }

}
