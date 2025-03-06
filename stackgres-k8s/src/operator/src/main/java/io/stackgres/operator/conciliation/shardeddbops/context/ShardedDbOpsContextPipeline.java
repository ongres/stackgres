/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class ShardedDbOpsContextPipeline
    extends ContextPipeline<StackGresShardedDbOps, Builder> {

  public ShardedDbOpsContextPipeline(Instance<ContextAppender<StackGresShardedDbOps, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
