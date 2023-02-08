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
import io.stackgres.common.LabelFactoryForBackup;
import io.stackgres.common.crd.sgbackup.BackupStatus;
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
      LabelFactoryForBackup labelFactory,
      ResourceFinder<Job> jobFinder,
      ResourceWriter<Job> jobWriter,
      ResourceScanner<Pod> podScanner,
      ResourceWriter<Pod> podWriter) {
    super(labelFactory, jobFinder, jobWriter, podScanner, podWriter);
  }

  @Override
  protected boolean isAlreadyCompleted(StackGresBackup context) {
    return Optional.of(context)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .filter(status -> BackupStatus.COMPLETED.status().equals(status)
            || BackupStatus.FAILED.status().equals(status))
        .isPresent();
  }

}
