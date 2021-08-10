/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;

public abstract class ConstraintValidator<T extends AdmissionReview<?>> implements Validator<T> {

  private static final String NAME_CONSTRAINT_MESSAGE = "Valid name must be 53 characters or less";
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
    if (target == null) {
      return;
    }

    validateResourceNameSizeConstraint(target);

    Set<ConstraintViolation<Object>> violations = constraintValidator.validate(target);
    if (!violations.isEmpty()) {
      buildValidationFailed(target, violations);
    }
  }

  private void buildValidationFailed(final HasMetadata target,
      Set<ConstraintViolation<Object>> violations) throws ValidationFailed {
    StatusDetailsBuilder detailsBuilder = new StatusDetailsBuilder();

    violations.forEach(violation -> {
      final List<String> fields = getOffendingFields(violation);
      final String reason = violation
          .getConstraintDescriptor()
          .getAnnotation().annotationType()
          .getName();
      fields.forEach(field -> detailsBuilder
          .addNewCause(field, violation.getMessage(), reason));
    });

    if (violations.stream().map(this::getOffendingFields)
        .flatMap(List::stream).count() == 1) {
      violations.forEach(violation -> detailsBuilder
          .withName(getOffendingFields(violation).get(0)));
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

  /**
   * Names must not be longer than valid labels. Follows documentation
   * https://kubernetes.io/docs/concepts/overview/working-with-objects/labels
   *
   * @param target HashMetadata
   * @throws ValidationFailed Validation message regarding name convention
   */
  private void validateResourceNameSizeConstraint(final HasMetadata target)
      throws ValidationFailed {

    try {
      Preconditions.checkArgument(target.getMetadata().getName().length() <= 53,
          NAME_CONSTRAINT_MESSAGE);
      ResourceUtil.resourceName(target.getMetadata().getName());
    } catch (IllegalArgumentException e) {
      throw new ValidationFailed(e.getMessage());
    }
  }

  private List<String> getOffendingFields(ConstraintViolation<Object> violation) {
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
          .collect(ImmutableList.toImmutableList());
    }
    return ImmutableList.of(propertyPath);
  }

  @Inject
  public void setConstraintValidator(javax.validation.Validator constraintValidator) {
    this.constraintValidator = constraintValidator;
  }
}
