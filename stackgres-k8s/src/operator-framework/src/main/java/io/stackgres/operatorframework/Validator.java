/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework;

public interface Validator<T> {

  void validate(T review) throws ValidationFailed;

  /**
   * Check value exists and is not empty.
   */
  default void checkIfProvided(String value, String field) throws ValidationFailed {
    if (value == null || value.isEmpty()) {
      throw new ValidationFailed(field + " must be provided");
    }
  }
}
