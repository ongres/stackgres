/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface DbOpsMutator  extends JsonPatchMutator<DbOpsReview> {
}
