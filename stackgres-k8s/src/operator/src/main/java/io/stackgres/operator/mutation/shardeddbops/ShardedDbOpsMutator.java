/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface ShardedDbOpsMutator
    extends Mutator<StackGresShardedDbOps, ShardedDbOpsReview> {
}
