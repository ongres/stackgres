/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ShardedDbOpsPipeline
    extends AbstractMutationPipeline<StackGresShardedDbOps, ShardedDbOpsReview> {

  @Inject
  public ShardedDbOpsPipeline(
      @Any Instance<ShardedDbOpsMutator> mutators) {
    super(mutators);
  }

}
