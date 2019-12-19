/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.profile;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operatorframework.JsonPatchMutator;

public interface ProfileMutator extends JsonPatchMutator<SgProfileReview> {

  JsonPointer SG_PROFILE_CONFIG_POINTER = JsonPointer.of("spec");

}
