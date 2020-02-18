/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface PgBouncerMutator extends JsonPatchMutator<PgBouncerReview> {

  JsonPointer PG_BOUNCER_CONFIG_POINTER = JsonPointer.of("spec", "pgbouncer.ini");
}
