/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import java.util.Optional;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupClusterContextAppender
    extends ContextAppender<StackGresBackup, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final BackupClusterInstanceProfileContextAppender backupClusterInstanceProfileContextAppender;
  private final BackupClusterObjectStorageContextAppender backupClusterObjectStorageContextAppender;

  public BackupClusterContextAppender(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      BackupClusterInstanceProfileContextAppender backupClusterInstanceProfileContextAppender,
      BackupClusterObjectStorageContextAppender backupClusterObjectStorageContextAppender) {
    this.clusterFinder = clusterFinder;
    this.backupClusterInstanceProfileContextAppender = backupClusterInstanceProfileContextAppender;
    this.backupClusterObjectStorageContextAppender = backupClusterObjectStorageContextAppender;
  }

  @Override
  public void appendContext(StackGresBackup backup, Builder contextBuilder) {
    final String clusterNamespace = StackGresUtil.getNamespaceFromRelativeId(
        backup.getSpec().getSgCluster(), backup.getMetadata().getNamespace());
    final String clusterName = StackGresUtil.getNameFromRelativeId(
        backup.getSpec().getSgCluster());
    if (BackupStatus.isFinished(backup)
        && !StackGresUtil.isRelativeIdNotInSameNamespace(backup.getSpec().getSgCluster())) {
      contextBuilder.foundCluster(Optional.empty());
      return;
    }

    final Optional<StackGresCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(
            clusterName,
            clusterNamespace);

    contextBuilder.foundCluster(foundCluster);

    if (foundCluster.isEmpty()) {
      if (StackGresUtil.isRelativeIdNotInSameNamespace(backup.getSpec().getSgCluster())) {
        return;
      }
      throw new IllegalArgumentException(
          StackGresCluster.KIND + " " + clusterName + " was not found");
    }
    final StackGresCluster cluster = foundCluster.get();
    backupClusterInstanceProfileContextAppender.appendContext(cluster, contextBuilder);
    backupClusterObjectStorageContextAppender.appendContext(cluster, contextBuilder);
  }

}
