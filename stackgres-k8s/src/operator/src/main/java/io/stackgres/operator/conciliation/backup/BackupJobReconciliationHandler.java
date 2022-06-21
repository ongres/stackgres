/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.AbstractJobReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresBackup.class, kind = "Job")
@ApplicationScoped
public class BackupJobReconciliationHandler
    extends AbstractJobReconciliationHandler<StackGresBackup> {

  @Inject
  public BackupJobReconciliationHandler(
      LabelFactory<StackGresBackup> labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceWriter<Job> jobWriter,
      ResourceScanner<Pod> podScanner,
      ResourceWriter<Pod> podWriter) {
    super(labelFactory, jobFinder, jobWriter, podScanner, podWriter);
  }

  public BackupJobReconciliationHandler() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected boolean isAlreadyCompleted(StackGresBackup context) {
    return Optional.of(context)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .filter(status -> BackupPhase.COMPLETED.label().equals(status)
            || BackupPhase.FAILED.label().equals(status))
        .isPresent();
  }

}
