/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class AbstractConstraintValidator<T extends AdmissionReview<?>>
    implements Validator<T> {

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
          final List<String> fields = ValidationUtil.getOffendingFields(
              review.getRequest().getObject(), violation);
          final String reason = violation
              .getConstraintDescriptor()
              .getAnnotation().annotationType()
              .getName();
          fields.stream().sorted()
              .forEach(field -> detailsBuilder
                  .addNewCause(field, violation.getMessage(), reason));
        });
        if (violations.stream().map(violation -> ValidationUtil.getOffendingFields(
            review.getRequest().getObject(), violation))
            .flatMap(List::stream).count() == 1) {
          violations.forEach(violation -> detailsBuilder
              .withName(ValidationUtil.getOffendingFields(
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

  @Inject
  public void setConstraintValidator(javax.validation.Validator constraintValidator) {
    this.constraintValidator = constraintValidator;
  }
}
