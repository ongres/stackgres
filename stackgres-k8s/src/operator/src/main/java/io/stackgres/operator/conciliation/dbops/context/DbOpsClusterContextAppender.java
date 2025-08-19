/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import java.util.Optional;

import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DbOpsClusterContextAppender
    extends ContextAppender<StackGresDbOps, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;
  private final DbOpsClusterInstanceProfileContextAppender dbOpsClusterInstanceProfileContextAppender;
  private final DbOpsClusterMajorVersionUpgradeContextAppender dbOpsClusterMajorVersionUpgradeContextAppender;
  private final DbOpsClusterMinorVersionUpgradeContextAppender dbOpsClusterMinorVersionUpgradeContextAppender;

  public DbOpsClusterContextAppender(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      DbOpsClusterInstanceProfileContextAppender dbOpsClusterInstanceProfileContextAppender,
      DbOpsClusterMajorVersionUpgradeContextAppender dbOpsClusterMajorVersionUpgradeContextAppender,
      DbOpsClusterMinorVersionUpgradeContextAppender dbOpsClusterMinorVersionUpgradeContextAppender) {
    this.clusterFinder = clusterFinder;
    this.dbOpsClusterInstanceProfileContextAppender = dbOpsClusterInstanceProfileContextAppender;
    this.dbOpsClusterMajorVersionUpgradeContextAppender = dbOpsClusterMajorVersionUpgradeContextAppender;
    this.dbOpsClusterMinorVersionUpgradeContextAppender = dbOpsClusterMinorVersionUpgradeContextAppender;
  }

  @Override
  public void appendContext(StackGresDbOps dbOps, Builder contextBuilder) {
    if (DbOpsUtil.isAlreadyCompleted(dbOps)) {
      contextBuilder.foundCluster(Optional.empty());
      return;
    }

    final Optional<StackGresCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(
            dbOps.getSpec().getSgCluster(),
            dbOps.getMetadata().getNamespace());

    contextBuilder.foundCluster(foundCluster);

    if (foundCluster.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresCluster.KIND + " " + dbOps.getSpec().getSgCluster() + " was not found");
    }
    final StackGresCluster cluster = foundCluster.get();
    dbOpsClusterInstanceProfileContextAppender.appendContext(cluster, contextBuilder);
    if (dbOps.getSpec().isOpMajorVersionUpgrade()) {
      dbOpsClusterMajorVersionUpgradeContextAppender.appendContext(dbOps, cluster, contextBuilder);
    }
    if (dbOps.getSpec().isOpMinorVersionUpgrade()) {
      dbOpsClusterMinorVersionUpgradeContextAppender.appendContext(dbOps, cluster, contextBuilder);
    }
  }

}
