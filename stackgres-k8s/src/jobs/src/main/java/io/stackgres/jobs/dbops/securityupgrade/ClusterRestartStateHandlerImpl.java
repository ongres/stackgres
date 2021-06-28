/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.StateHandler;

@ApplicationScoped
@StateHandler("securityUpgrade")
public class ClusterRestartStateHandlerImpl extends AbstractRestartStateHandler {

  @Override
  protected DbOpsRestartStatus getDbOpRestartStatus(StackGresDbOps dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresDbOpsStatus::getSecurityUpgrade)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresDbOpsStatus());
          }
          dbOps.getStatus().setSecurityUpgrade(new StackGresDbOpsSecurityUpgradeStatus());

          return dbOps.getStatus().getSecurityUpgrade();
        });
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresClusterStatus());
          }
          if (dbOps.getStatus().getDbOps() == null) {
            dbOps.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
          }
          if (dbOps.getStatus().getDbOps().getSecurityUpgrade() == null) {
            dbOps.getStatus().getDbOps()
                .setSecurityUpgrade(new StackGresClusterDbOpsSecurityUpgradeStatus());
          }
          return dbOps.getStatus().getDbOps().getSecurityUpgrade();
        });
  }

  @Override
  protected void cleanClusterStatus(StackGresCluster cluster) {
    cluster.getStatus().setDbOps(null);
  }

  @Override
  protected boolean isSgClusterDbOpsStatusInitialized(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
        .filter(status -> status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty()
            && status.getPrimaryInstance() != null)
        .isPresent();
  }

  @Override
  protected boolean isDbOpsStatusInitialized(StackGresDbOps cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresDbOpsStatus::getSecurityUpgrade)
        .filter(status -> status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty()
            && status.getPrimaryInstance() != null)
        .isPresent();
  }

  @Override
  protected Optional<String> getRestartMethod(StackGresDbOps op) {
    return Optional.ofNullable(op.getSpec())
        .map(StackGresDbOpsSpec::getSecurityUpgrade)
        .map(StackGresDbOpsSecurityUpgrade::getMethod);
  }

}
