/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.FireAndForgetJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresBackup.class, kind = "Job")
@ApplicationScoped
public class BackupJobReconciliationHandler
    extends FireAndForgetJobReconciliationHandler<StackGresBackup> {

  @Inject
  public BackupJobReconciliationHandler(
      @ReconciliationScope(value = StackGresBackup.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresBackup> handler,
      ResourceFinder<Job> jobFinder,
      ResourceScanner<Pod> podScanner) {
    super(handler, jobFinder, podScanner);
  }

  @Override
  protected boolean canForget(StackGresBackup context, HasMetadata resource) {
    return BackupStatus.isFinished(context);
  }

}
