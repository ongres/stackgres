/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.labels.LabelFactoryForShardedBackup;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.FireAndForgetJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresBackup.class, kind = "Job")
@ApplicationScoped
public class ShardedBackupJobReconciliationHandler
    extends FireAndForgetJobReconciliationHandler<StackGresShardedBackup> {

  @Inject
  public ShardedBackupJobReconciliationHandler(
      @ReconciliationScope(value = StackGresShardedBackup.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresShardedBackup> handler,
      LabelFactoryForShardedBackup labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler, labelFactory, jobFinder, podScanner);
  }

  @Override
  protected boolean canForget(StackGresShardedBackup context, HasMetadata resource) {
    return Optional.of(context)
        .map(StackGresShardedBackup::getStatus)
        .map(StackGresShardedBackupStatus::getProcess)
        .map(StackGresShardedBackupProcess::getStatus)
        .filter(status -> ShardedBackupStatus.COMPLETED.status().equals(status)
            || ShardedBackupStatus.FAILED.status().equals(status))
        .isPresent();
  }

}
