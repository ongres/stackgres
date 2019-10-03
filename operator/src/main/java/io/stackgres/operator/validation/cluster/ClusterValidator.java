/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operator.validation.ValidationFailed;

public interface ClusterValidator {

  void validate(StackgresClusterReview review) throws ValidationFailed;

  default void checkIfProvided(String value, String field) throws ValidationFailed {
    if (value == null || value.isEmpty()) {
      throw new ValidationFailed(field + " must be provided");
    }
  }

}
