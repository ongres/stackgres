/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.common.ClassUtil;
import io.stackgres.common.ErrorType;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConstraintValidator<T extends AdmissionReview<?>> implements Validator<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintValidator.class);

  private javax.validation.Validator constraintValidator;

  private String constraintViolationDocumentationUri;

  @PostConstruct
  public void init() {
    constraintViolationDocumentationUri = ErrorType
        .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);
  }

  @Override
  public void validate(T review) throws ValidationFailed {
    final HasMetadata target = review.getRequest().getObject();

    if (target != null) {
      Set<ConstraintViolation<Object>> violations = constraintValidator.validate(target);
      if (!violations.isEmpty()) {
        StatusDetailsBuilder detailsBuilder = new StatusDetailsBuilder();

        violations.forEach(violation -> {
          final List<String> fields = getOffendingFields(
              review.getRequest().getObject(), violation);
          final String reason = violation
              .getConstraintDescriptor()
              .getAnnotation().annotationType()
              .getName();
          fields.stream().sorted()
              .forEach(field -> detailsBuilder
                  .addNewCause(field, violation.getMessage(), reason));
        });
        if (violations.stream().map(violation -> getOffendingFields(
            review.getRequest().getObject(), violation))
            .flatMap(List::stream).count() == 1) {
          violations.forEach(violation -> detailsBuilder
              .withName(getOffendingFields(
                  review.getRequest().getObject(), violation).get(0)));
        }
        StatusDetails details = detailsBuilder.build();
        Status status = new StatusBuilder()
            .withCode(422)
            .withMessage(target.getKind() + " has invalid properties. "
                + details.getCauses().get(0).getMessage())
            .withKind(target.getKind())
            .withReason(constraintViolationDocumentationUri)
            .withDetails(details)
            .build();
        throw new ValidationFailed(status);
      }
    }
  }

  private List<String> getOffendingFields(
      final Object object, final ConstraintViolation<Object> violation) {
    final String propertyPath = violation.getPropertyPath().toString();
    if (Seq.seq(violation.getConstraintDescriptor().getPayload())
        .map(fieldReference -> Optional.ofNullable(fieldReference
            .getAnnotation(FieldReference.ReferencedField.class)))
        .anyMatch(Optional::isPresent)) {
      final String basePath = propertyPath.substring(0, propertyPath.lastIndexOf('.'));
      return Seq.seq(violation.getConstraintDescriptor().getPayload())
          .map(fieldReference -> fieldReference
              .getAnnotation(FieldReference.ReferencedField.class))
          .map(FieldReference.ReferencedField::value)
          .map(field -> basePath + "." + field)
          .map(field -> replacePropertyPathWithJsonProperties(object, field))
          .collect(ImmutableList.toImmutableList());
    }
    return ImmutableList.of(replacePropertyPathWithJsonProperties(object, propertyPath));
  }

  private String replacePropertyPathWithJsonProperties(
      final Object object, final String propertyPath) {
    try {
      return Arrays.asList(propertyPath.split("\\.")).stream()
          .reduce(
              Tuple.<StringBuilder, Class<?>>tuple(new StringBuilder(), object.getClass()),
              this::replacePropertyPathWithJsonProperties,
              (u, v) -> v)
          .v1().toString();
    } catch (Exception ex) {
      LOGGER.warn("Can not translate path " + propertyPath + " using @JsonProperty annotations",
          ex);
      return propertyPath;
    }
  }

  private Tuple2<StringBuilder, Class<?>> replacePropertyPathWithJsonProperties(
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
        .map(name -> name.replace(".", "\\.").replace("[", "\\["))
        .orElse(field);
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

  @Inject
  public void setConstraintValidator(javax.validation.Validator constraintValidator) {
    this.constraintValidator = constraintValidator;
  }
}
