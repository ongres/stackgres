/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import org.jetbrains.annotations.NotNull;

public interface Validator<T> {

  void validate(T review) throws ValidationFailed;

  /**
   * Check value exists and is not empty.
   */
  default void checkIfProvided(String value, @NotNull String field) throws ValidationFailed {
    if (value == null || value.isEmpty()) {
      throw new ValidationFailed(field + " must be provided");
    }
  }

  default void fail(String kind, String reason, String message) throws ValidationFailed {
    Status status = new StatusBuilder()
        .withMessage(message)
        .withKind(kind)
        .withCode(400)
        .withReason(reason)
        .build();
    throw new ValidationFailed(status);
  }
}
