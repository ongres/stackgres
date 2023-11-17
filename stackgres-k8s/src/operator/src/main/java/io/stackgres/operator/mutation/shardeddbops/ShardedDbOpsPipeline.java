/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsPipeline
    extends AbstractMutationPipeline<StackGresShardedDbOps, ShardedDbOpsReview> {

  @Inject
  public ShardedDbOpsPipeline(
      @Any Instance<ShardedDbOpsMutator> mutators) {
    super(mutators);
  }

}
