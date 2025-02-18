/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import java.util.Optional;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupClusterContextAppender
    extends ContextAppender<StackGresShardedBackup, Builder> {

  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;
  private final ShardedBackupClusterInstanceProfileContextAppender shardedBackupClusterInstanceProfileContextAppender;
  private final ShardedBackupClusterObjectStorageContextAppender shardedBackupClusterObjectStorageContextAppender;

  public ShardedBackupClusterContextAppender(
      CustomResourceFinder<StackGresShardedCluster> clusterFinder,
      ShardedBackupClusterInstanceProfileContextAppender shardedBackupClusterInstanceProfileContextAppender,
      ShardedBackupClusterObjectStorageContextAppender shardedBackupClusterObjectStorageContextAppender) {
    this.clusterFinder = clusterFinder;
    this.shardedBackupClusterInstanceProfileContextAppender = shardedBackupClusterInstanceProfileContextAppender;
    this.shardedBackupClusterObjectStorageContextAppender = shardedBackupClusterObjectStorageContextAppender;
  }

  @Override
  public void appendContext(StackGresShardedBackup backup, Builder contextBuilder) {
    final String clusterNamespace = StackGresUtil.getNamespaceFromRelativeId(
        backup.getSpec().getSgShardedCluster(), backup.getMetadata().getNamespace());
    if (!clusterNamespace.equals(backup.getMetadata().getNamespace())) {
      contextBuilder.foundShardedCluster(Optional.empty());
      return;
    }

    if (ShardedBackupStatus.isFinished(backup)) {
      contextBuilder.foundShardedCluster(Optional.empty());
      return;
    }

    final Optional<StackGresShardedCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(
            backup.getSpec().getSgShardedCluster(),
            backup.getMetadata().getNamespace());

    contextBuilder.foundShardedCluster(foundCluster);

    if (foundCluster.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresShardedCluster.KIND + " " + backup.getSpec().getSgShardedCluster() + " was not found");
    }
    final StackGresShardedCluster cluster = foundCluster.get();

    if (Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getType)
        .map(StackGresShardingType.SHARDING_SPHERE.toString()::equals)
        .orElse(false)) {
      throw new IllegalArgumentException(
          StackGresShardedCluster.KIND + " " + backup.getSpec().getSgShardedCluster()
          + " do not support sharded backups");
    }

    shardedBackupClusterInstanceProfileContextAppender.appendContext(cluster, contextBuilder);
    shardedBackupClusterObjectStorageContextAppender.appendContext(cluster, contextBuilder);
  }

}
