/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import jakarta.validation.ConstraintViolation;

public class ValidationFailed extends Exception {

  private static final long serialVersionUID = 6127484041987197268L;

  private final Status result;

  public ValidationFailed(@Nonnull Status status) {
    super(status.getMessage());
    this.result = status;
  }

  public ValidationFailed(@Nonnull String message, int code) {
    this(new StatusBuilder()
        .withCode(code)
        .withMessage(message)
        .build());
  }

  public ValidationFailed(@Nonnull String message) {
    this(message, 400);
  }

  /**
   * Create a {@code ValidationFailed} instance.
   */
  public ValidationFailed(Set<? extends ConstraintViolation<?>> violations) {
    this(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", ")));
  }

  public Status getResult() {
    return result;
  }

}
