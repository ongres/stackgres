/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.JsonPatchMutator;

public interface PgConfigMutator extends JsonPatchMutator<PgConfigReview> {

  JsonPointer PG_CONFIG_POINTER = JsonPointer.of("spec", "postgresql.conf");

}
