/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.JobUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelMapperForShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.factory.shardedbackup.ShardedBackupJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedBackupStatusManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShardedBackupStatusManager.class);

  private final CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder;
  private final LabelMapperForShardedCluster clusterLabelMapper;
  private final ResourceFinder<Job> jobFinder;

  @Inject
  public ShardedBackupStatusManager(
      CustomResourceFinder<StackGresShardedCluster> shardedClusterFinder,
      LabelMapperForShardedCluster clusterLabelMapper,
      ResourceFinder<Job> jobFinder) {
    this.shardedClusterFinder = shardedClusterFinder;
    this.clusterLabelMapper = clusterLabelMapper;
    this.jobFinder = jobFinder;
  }

  public StackGresShardedBackup refreshCondition(StackGresShardedBackup source) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresShardedBackupStatus());
    }
    if (source.getStatus().getProcess() == null) {
      source.getStatus().setProcess(new StackGresShardedBackupProcess());
    }
    Optional<StackGresShardedCluster> cluster = getShardedBackupShardedCluster(source);
    Optional<Job> backupJob = getShardedBackupJob(source, cluster);
    Optional<Boolean> isShardedBackupJobCompletedOrFailed =
        JobUtil.isJobCompleteOrFailed(backupJob);
    Optional<Boolean> isShardedBackupJobActive = JobUtil.isJobActive(backupJob);
    if (isShardedBackupJobCompletedOrFailed.isPresent()) {
      if (Boolean.TRUE.equals(isShardedBackupJobCompletedOrFailed.get())) {
        setStatus(source, ShardedBackupStatus.COMPLETED);
      } else {
        setStatus(source, ShardedBackupStatus.FAILED);
      }
    } else if (Boolean.TRUE.equals(isShardedBackupJobActive.orElse(false))) {
      setStatus(source, ShardedBackupStatus.RUNNING);
    } else if (isShardedBackupStatusNotInitialized(source)) {
      setStatus(source, ShardedBackupStatus.PENDING);
    }
    return source;
  }

  protected void setStatus(StackGresShardedBackup source, ShardedBackupStatus status) {
    if (!Objects.equals(
        source.getStatus().getProcess().getStatus(),
        status.status())) {
      LOGGER.debug("Sharded Backup {}.{} is {}",
          source.getMetadata().getNamespace(),
          source.getMetadata().getName(),
          status.status());
      source.getStatus().getProcess().setStatus(status.status());
    }
  }

  private Optional<StackGresShardedCluster> getShardedBackupShardedCluster(
      StackGresShardedBackup source) {
    return shardedClusterFinder.findByNameAndNamespace(
        source.getSpec().getSgShardedCluster(),
        source.getMetadata().getNamespace());
  }

  private Optional<Job> getShardedBackupJob(StackGresShardedBackup source,
      Optional<StackGresShardedCluster> cluster) {
    if (cluster.isEmpty()) {
      return Optional.empty();
    }
    final String scheduledShardedBackupKey =
        clusterLabelMapper.scheduledShardedBackupKey(cluster.get());
    final String scheduledShardedBackupJobNameKey =
        clusterLabelMapper.scheduledShardedBackupJobNameKey(
        cluster.get());
    return Optional.ofNullable(source.getMetadata().getLabels())
        .filter(labels -> labels.containsKey(scheduledShardedBackupKey))
        .filter(labels -> Objects.equals(
            labels.get(scheduledShardedBackupKey),
            StackGresContext.RIGHT_VALUE))
        .filter(labels -> labels.containsKey(scheduledShardedBackupJobNameKey))
        .flatMap(labels -> jobFinder.findByNameAndNamespace(
            labels.get(scheduledShardedBackupJobNameKey),
            source.getMetadata().getNamespace()))
        .or(() -> jobFinder.findByNameAndNamespace(
            ShardedBackupJob.backupJobName(source),
            source.getMetadata().getNamespace()));
  }

  private boolean isShardedBackupStatusNotInitialized(StackGresShardedBackup source) {
    return Optional.of(source)
        .map(StackGresShardedBackup::getStatus)
        .map(StackGresShardedBackupStatus::getProcess)
        .map(StackGresShardedBackupProcess::getStatus)
        .isEmpty();
  }

}
