/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.common.DbOpsUtil;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterRolloutDbOpsContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private final CustomResourceFinder<StackGresDbOps> dbOpsFinder;

  public ClusterRolloutDbOpsContextAppender(
      CustomResourceFinder<StackGresDbOps> dbOpsFinder) {
    this.dbOpsFinder = dbOpsFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<String> rolloutDbOps =
        Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getName);
    if (rolloutDbOps
        .map(name -> dbOpsFinder
            .findByNameAndNamespace(
                name,
                cluster.getMetadata().getNamespace())
            .filter(dbOp -> DbOpsUtil.ROLLOUT_OPS.contains(dbOp.getSpec().getOp()))
            .isEmpty())
        .orElse(false)) {
      cluster.getStatus().getDbOps().setName(ClusterRolloutUtil.DBOPS_NOT_FOUND_NAME);
    }
  }

}
