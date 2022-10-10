/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobCondition;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.factory.backup.BackupJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupStatusManager {

  private static final String COMPLETE_JOB_CONDITION_TYPES = "Complete";
  private static final String FAILED_JOB_CONDITION_TYPES = "Failed";
  private static final List<String> COMPLETE_AND_FAILED_JOB_CONDITION_TYPES =
      List.of(COMPLETE_JOB_CONDITION_TYPES, FAILED_JOB_CONDITION_TYPES);

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupStatusManager.class);

  private final ResourceFinder<Job> jobFinder;

  @Inject
  public BackupStatusManager(ResourceFinder<Job> jobFinder) {
    this.jobFinder = jobFinder;
  }

  public StackGresBackup refreshCondition(StackGresBackup source) {
    if (isBackupStatusNotInitialized(source)) {
      LOGGER.debug("Backup {}.{} is not initialized",
          source.getMetadata().getNamespace(),
          source.getMetadata().getName());
      if (source.getStatus() == null) {
        source.setStatus(new StackGresBackupStatus());
      }
      if (source.getStatus().getProcess() == null) {
        source.getStatus().setProcess(new StackGresBackupProcess());
      }
      source.getStatus().getProcess().setStatus(BackupStatus.PENDING.status());
    }
    Optional<Boolean> backupJobCompletedOrFailed = isBackupJobCompleteOrFailed(source);
    if (backupJobCompletedOrFailed.isPresent()) {
      if (source.getStatus() == null) {
        source.setStatus(new StackGresBackupStatus());
      }
      if (source.getStatus().getProcess() == null) {
        source.getStatus().setProcess(new StackGresBackupProcess());
      }
      if (Boolean.TRUE.equals(backupJobCompletedOrFailed.get())) {
        source.getStatus().getProcess().setStatus(BackupStatus.COMPLETED.status());
      } else {
        source.getStatus().getProcess().setStatus(BackupStatus.FAILED.status());
      }
    }
    return source;
  }

  private boolean isBackupStatusNotInitialized(StackGresBackup source) {
    return Optional.of(source)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .isEmpty();
  }

  private Optional<Boolean> isBackupJobCompleteOrFailed(StackGresBackup source) {
    return jobFinder.findByNameAndNamespace(BackupJob.backupJobName(source),
        source.getMetadata().getNamespace())
        .map(Job::getStatus)
        .map(JobStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> COMPLETE_AND_FAILED_JOB_CONDITION_TYPES.contains(condition.getType()))
        .filter(condition -> condition.getStatus().equals("True"))
        .map(JobCondition::getType)
        .map(COMPLETE_JOB_CONDITION_TYPES::equals)
        .findFirst();
  }

}
