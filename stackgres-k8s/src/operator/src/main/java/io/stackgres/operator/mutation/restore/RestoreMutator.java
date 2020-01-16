/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.restore;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operatorframework.JsonPatchMutator;

public interface RestoreMutator extends JsonPatchMutator<RestoreConfigReview> {

  JsonPointer SG_RESTORE_CONFIG_POINTER = JsonPointer.of("spec");
}
