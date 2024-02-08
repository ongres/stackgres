/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.FireAndForgetJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresShardedDbOps.class, kind = "Job")
@ApplicationScoped
public class ShardedDbOpsJobReconciliationHandler
    extends FireAndForgetJobReconciliationHandler<StackGresShardedDbOps> {

  @Inject
  public ShardedDbOpsJobReconciliationHandler(
      @ReconciliationScope(value = StackGresShardedDbOps.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresShardedDbOps> handler,
      LabelFactoryForShardedDbOps labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler, labelFactory, jobFinder, podScanner);
  }

  @Override
  protected boolean canForget(StackGresShardedDbOps context, HasMetadata resource) {
    return ShardedDbOpsUtil.isAlreadyCompleted(context);
  }

}
