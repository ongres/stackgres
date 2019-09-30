/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.stackgres.operator.validation.Result;

public class ValidationFailed extends Exception {

  private Result result;

  public ValidationFailed(String message) {
    super(message);
    result = new Result(500, message);
  }

  public Result getResult() {
    return result;
  }

}
