/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.StateHandler;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@StateHandler("securityUpgrade")
public class SecurityUpgradeStateHandler extends AbstractRestartStateHandler {

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
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void setDbOpRestartStatus(StackGresDbOps dbOps, DbOpsRestartStatus dbOpsStatus) {
    dbOps.getStatus().setSecurityUpgrade((StackGresDbOpsSecurityUpgradeStatus) dbOpsStatus);
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade)
        .orElseGet(() -> {
          if (cluster.getStatus() == null) {
            cluster.setStatus(new StackGresClusterStatus());
          }
          if (cluster.getStatus().getDbOps() == null) {
            cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
          }
          if (cluster.getStatus().getDbOps().getSecurityUpgrade() == null) {
            cluster.getStatus().getDbOps()
                .setSecurityUpgrade(new StackGresClusterDbOpsSecurityUpgradeStatus());
          }
          return cluster.getStatus().getDbOps().getSecurityUpgrade();
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
            && !status.getInitialInstances().isEmpty())
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
  protected Optional<DbOpsMethodType> getRestartMethod(StackGresDbOps op) {
    return Optional.ofNullable(op.getSpec())
        .map(StackGresDbOpsSpec::getSecurityUpgrade)
        .map(StackGresDbOpsSecurityUpgrade::getMethod)
        .map(DbOpsMethodType::fromString);
  }

}
