/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface DbOpsMutator extends Mutator<StackGresDbOps, DbOpsReview> {
}
