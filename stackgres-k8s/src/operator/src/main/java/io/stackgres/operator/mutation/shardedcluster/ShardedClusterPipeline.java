/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ShardedClusterPipeline
    extends AbstractMutationPipeline<StackGresShardedCluster, StackGresShardedClusterReview> {

  @Inject
  public ShardedClusterPipeline(
      @Any Instance<ShardedClusterMutator> mutators) {
    super(mutators);
  }

}
