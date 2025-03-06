/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;

import java.util.Optional;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedbackup.ShardedBackupStatus;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedBackupClusterCoordinatorContextAppender
    extends ContextAppender<StackGresShardedBackup, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  public ShardedBackupClusterCoordinatorContextAppender(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  public void appendContext(StackGresShardedBackup backup, Builder contextBuilder) {
    final String clusterNamespace = StackGresUtil.getNamespaceFromRelativeId(
        backup.getSpec().getSgShardedCluster(), backup.getMetadata().getNamespace());
    if (!clusterNamespace.equals(backup.getMetadata().getNamespace())) {
      contextBuilder.foundCoordinator(Optional.empty());
      return;
    }

    if (ShardedBackupStatus.isFinished(backup)) {
      contextBuilder.foundCoordinator(Optional.empty());
      return;
    }

    final String coordinatorClusterName = getCoordinatorClusterName(backup.getSpec().getSgShardedCluster());
    final Optional<StackGresCluster> foundCoordinator = clusterFinder
        .findByNameAndNamespace(
            coordinatorClusterName,
            backup.getMetadata().getNamespace());
    if (foundCoordinator.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresCluster.KIND + " " + coordinatorClusterName + " was not found");
    }
    contextBuilder.foundCoordinator(foundCoordinator);
  }

}
