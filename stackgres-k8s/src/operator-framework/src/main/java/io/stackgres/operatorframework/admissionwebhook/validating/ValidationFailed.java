/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;

public class ValidationFailed extends Exception {

  private static final long serialVersionUID = 6127484041987197268L;

  private Status result;

  public ValidationFailed(String message) {
    super(message);
    result = new StatusBuilder()
        .withCode(500)
        .withMessage(message)
        .build();
  }

  public ValidationFailed(Status status) {
    super(status.getMessage());
    this.result = status;
  }

  /**
   * Create a {@code ValidationFailed} instance.
   */
  public ValidationFailed(Set<? extends ConstraintViolation<?>> violations) {
    super(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", ")));

    result = new StatusBuilder()
        .withCode(500)
        .withMessage(this.getMessage())
        .build();
  }

  public Status getResult() {
    return result;
  }

}
