/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class ConstraintValidator<T extends AdmissionReview<?>> implements Validator<T> {

  private javax.validation.Validator constraintValidator;

  private ConfigContext configContext;

  private String constraintViolationDocumentationUri;

  @PostConstruct
  public void init() {
    constraintViolationDocumentationUri = configContext
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
          final String field = getOffendingField(violation);
          final String reason = violation
              .getConstraintDescriptor()
              .getAnnotation().annotationType()
              .getName();
          detailsBuilder.addNewCause(field, violation.getMessage(), reason);
        });
        if (violations.size() == 1) {
          violations.forEach(violation -> detailsBuilder
              .withName(getOffendingField(violation)));
        }
        Status status = new StatusBuilder()
            .withCode(422)
            .withMessage(target.getKind() + " has invalid properties")
            .withKind(target.getKind())
            .withReason(constraintViolationDocumentationUri)
            .withDetails(detailsBuilder.build())
            .build();
        throw new ValidationFailed(status);
      }
    }
  }

  private static String getOffendingField(ConstraintViolation<Object> violation) {
    return violation.getPropertyPath().toString();
  }

  @Inject
  public void setConfigContext(ConfigContext configContext) {
    this.configContext = configContext;
  }

  @Inject
  public void setConstraintValidator(javax.validation.Validator constraintValidator) {
    this.constraintValidator = constraintValidator;
  }
}
