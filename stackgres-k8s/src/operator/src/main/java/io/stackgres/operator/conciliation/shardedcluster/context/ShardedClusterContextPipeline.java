/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class ShardedClusterContextPipeline
    extends ContextPipeline<StackGresShardedCluster, Builder> {

  public ShardedClusterContextPipeline(Instance<ContextAppender<StackGresShardedCluster, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
