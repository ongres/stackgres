/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface PgBouncerMutator extends JsonPatchMutator<PoolingReview> {

  JsonPointer PG_BOUNCER_CONFIG_POINTER = JsonPointer.of("spec",
      "pgBouncer",
      "pgbouncer.ini");

  JsonPointer PG_BOUNCER_DEFAULT_PARAMETERS_POINTER = JsonPointer.of("status",
      "pgBouncer",
      "defaultParameters");
}
