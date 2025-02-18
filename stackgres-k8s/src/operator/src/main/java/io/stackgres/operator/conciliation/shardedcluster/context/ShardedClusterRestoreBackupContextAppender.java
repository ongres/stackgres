/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterInitialData;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestoreFromBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterRestoreBackupContextAppender
    extends ContextAppender<StackGresShardedCluster, Builder> {

  private final CustomResourceFinder<StackGresShardedBackup> backupFinder;

  public ShardedClusterRestoreBackupContextAppender(
      CustomResourceFinder<StackGresShardedBackup> backupFinder) {
    this.backupFinder = backupFinder;
  }

  @Override
  public void appendContext(StackGresShardedCluster cluster, Builder contextBuilder) {
    findRestoreBackup(
        cluster,
        cluster.getMetadata().getNamespace());

  }

  private void findRestoreBackup(
      StackGresShardedCluster cluster,
      final String clusterNamespace) {
    Optional<StackGresShardedBackup> foundRestoreBackup = Optional
        .ofNullable(cluster.getSpec().getInitialData())
        .map(StackGresShardedClusterInitialData::getRestore)
        .map(StackGresShardedClusterRestore::getFromBackup)
        .map(StackGresShardedClusterRestoreFromBackup::getName)
        .flatMap(backupName -> backupFinder.findByNameAndNamespace(backupName, clusterNamespace));
    if (foundRestoreBackup.isPresent()) {
      final var restoreBackup = foundRestoreBackup.get();
      if (!ShardedBackupStatus.isCompleted(restoreBackup)) {
        throw new IllegalArgumentException("Cannot restore from " + StackGresShardedBackup.KIND + " "
            + restoreBackup.getMetadata().getName()
            + " because it's not Completed");
      }

      String backupMajorVersion = restoreBackup
          .getStatus()
          .getBackupInformation()
          .getPostgresMajorVersion();

      String givenPgVersion = cluster.getSpec()
          .getPostgres().getVersion();
      String givenMajorVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getMajorVersion(givenPgVersion);

      if (!backupMajorVersion.equals(givenMajorVersion)) {
        throw new IllegalArgumentException("Cannot restore from " + StackGresShardedBackup.KIND + " "
            + restoreBackup.getMetadata().getName()
            + " because it has been created from a postgres instance"
            + " with version " + backupMajorVersion);
      }

      int clusters = 1 + cluster.getSpec().getShards().getClusters();
      var sgBackups = foundRestoreBackup
          .map(StackGresShardedBackup::getStatus)
          .map(StackGresShardedBackupStatus::getSgBackups)
          .orElse(null);
      if (!Optional.ofNullable(sgBackups)
          .map(list -> list.size() == clusters)
          .orElse(false)) {
        throw new IllegalArgumentException(
            "In " + StackGresShardedBackup.KIND + " " + restoreBackup.getMetadata().getName()
            + " sgBackups must be an array of size " + clusters
            + " (the coordinator plus the number of shards)"
            + " but was " + Optional.ofNullable(sgBackups)
            .map(List::size)
            .orElse(null));
      }
      if (cluster.getStatus() == null) {
        cluster.setStatus(new StackGresShardedClusterStatus());
      }
      cluster.getStatus().setSgBackups(sgBackups);
    }
  }

}
