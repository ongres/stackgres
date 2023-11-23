/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.JobUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelMapperForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.factory.backup.BackupJob;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupStatusManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupStatusManager.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final LabelMapperForCluster<StackGresCluster> clusterLabelMapper;
  private final ResourceFinder<Job> jobFinder;

  @Inject
  public BackupStatusManager(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      LabelMapperForCluster<StackGresCluster> clusterLabelMapper,
      ResourceFinder<Job> jobFinder) {
    this.clusterFinder = clusterFinder;
    this.clusterLabelMapper = clusterLabelMapper;
    this.jobFinder = jobFinder;
  }

  public StackGresBackup refreshCondition(StackGresBackup source) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresBackupStatus());
    }
    if (source.getStatus().getProcess() == null) {
      source.getStatus().setProcess(new StackGresBackupProcess());
    }
    Optional<StackGresCluster> cluster = getBackupCluster(source);
    Optional<Job> backupJob = getBackupJob(source, cluster);
    Optional<Boolean> isBackupJobCompletedOrFailed = JobUtil.isJobCompleteOrFailed(backupJob);
    Optional<Boolean> isBackupJobActive = JobUtil.isJobActive(backupJob);
    if (isBackupJobCompletedOrFailed.isPresent()) {
      if (Boolean.TRUE.equals(isBackupJobCompletedOrFailed.get())) {
        setStatus(source, BackupStatus.COMPLETED);
      } else {
        setStatus(source, BackupStatus.FAILED);
      }
    } else if (Boolean.TRUE.equals(isBackupJobActive.orElse(false))) {
      setStatus(source, BackupStatus.RUNNING);
    } else if (isBackupStatusNotInitialized(source)) {
      setStatus(source, BackupStatus.PENDING);
    }
    return source;
  }

  protected void setStatus(StackGresBackup source, BackupStatus status) {
    if (!Objects.equals(
        source.getStatus().getProcess().getStatus(),
        status.status())) {
      LOGGER.debug("Backup {}.{} is {}",
          source.getMetadata().getNamespace(),
          source.getMetadata().getName(),
          status.status());
      source.getStatus().getProcess().setStatus(status.status());
    }
  }

  private Optional<StackGresCluster> getBackupCluster(StackGresBackup source) {
    return clusterFinder.findByNameAndNamespace(
        source.getSpec().getSgCluster(),
        source.getMetadata().getNamespace());
  }

  private Optional<Job> getBackupJob(StackGresBackup source,
      Optional<StackGresCluster> cluster) {
    if (cluster.isEmpty()) {
      return Optional.empty();
    }
    final String scheduledBackupKey = clusterLabelMapper.scheduledBackupKey(cluster.get());
    final String scheduledBackupJobNameKey = clusterLabelMapper.scheduledBackupJobNameKey(
        cluster.get());
    return Optional.ofNullable(source.getMetadata().getLabels())
        .filter(labels -> labels.containsKey(scheduledBackupKey))
        .filter(labels -> Objects.equals(
            labels.get(scheduledBackupKey),
            StackGresContext.RIGHT_VALUE))
        .filter(labels -> labels.containsKey(scheduledBackupJobNameKey))
        .flatMap(labels -> jobFinder.findByNameAndNamespace(
            labels.get(scheduledBackupJobNameKey),
            source.getMetadata().getNamespace()))
        .or(() -> jobFinder.findByNameAndNamespace(
            BackupJob.backupJobName(source),
            source.getMetadata().getNamespace()));
  }

  private boolean isBackupStatusNotInitialized(StackGresBackup source) {
    return Optional.of(source)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .isEmpty();
  }

}
