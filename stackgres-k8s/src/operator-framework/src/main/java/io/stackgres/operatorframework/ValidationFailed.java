/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;

public class ValidationFailed extends Exception {

  private static final long serialVersionUID = 6127484041987197268L;

  private Result result;

  public ValidationFailed(String message) {
    super(message);
    result = new Result(500, message);
  }

  /**
   * Create a {@code ValidationFailed} instance.
   */
  public ValidationFailed(Set<? extends ConstraintViolation<?>> violations) {
    super(violations.stream()
        .map(cv -> cv.getMessage())
        .collect(Collectors.joining(", ")));
    result = new Result(500, this.getMessage());
  }

  public Result getResult() {
    return result;
  }

}
