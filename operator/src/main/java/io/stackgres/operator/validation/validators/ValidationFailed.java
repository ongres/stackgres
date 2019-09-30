/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.validators;

import io.stackgres.operator.validation.Result;

public class ValidationFailed extends Exception {

  private static final long serialVersionUID = 6127484041987197268L;

  private Result result;

  public ValidationFailed(String message) {
    super(message);
    result = new Result(500, message);
  }

  public Result getResult() {
    return result;
  }

}
