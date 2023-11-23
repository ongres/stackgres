/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.StateHandler;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@StateHandler("restart")
public class ClusterRestartStateHandler extends AbstractRestartStateHandler {

  @Override
  protected DbOpsRestartStatus getDbOpRestartStatus(StackGresDbOps dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresDbOpsStatus::getRestart)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresDbOpsStatus());
          }
          dbOps.getStatus().setRestart(new StackGresDbOpsRestartStatus());

          return dbOps.getStatus().getRestart();
        });
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void setDbOpRestartStatus(StackGresDbOps dbOps, DbOpsRestartStatus dbOpsStatus) {
    dbOps.getStatus().setRestart((StackGresDbOpsRestartStatus) dbOpsStatus);
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterRestartStatus(StackGresCluster dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getRestart)
        .orElseGet(() -> {
          if (dbOps.getStatus() == null) {
            dbOps.setStatus(new StackGresClusterStatus());
          }
          if (dbOps.getStatus().getDbOps() == null) {
            dbOps.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
          }
          if (dbOps.getStatus().getDbOps().getRestart() == null) {
            dbOps.getStatus().getDbOps()
                .setRestart(new StackGresClusterDbOpsRestartStatus());
          }
          return dbOps.getStatus().getDbOps().getRestart();
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
        .map(StackGresClusterDbOpsStatus::getRestart)
        .filter(status -> status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty()
            && status.getPrimaryInstance() != null)
        .isPresent();
  }

  @Override
  protected boolean isDbOpsStatusInitialized(StackGresDbOps cluster) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresDbOpsStatus::getRestart)
        .filter(status -> status.getInitialInstances() != null
            && !status.getInitialInstances().isEmpty()
            && status.getPrimaryInstance() != null)
        .isPresent();
  }

  @Override
  protected Optional<DbOpsMethodType> getRestartMethod(StackGresDbOps op) {
    return Optional.ofNullable(op.getSpec())
        .map(StackGresDbOpsSpec::getRestart)
        .map(StackGresDbOpsRestart::getMethod)
        .map(DbOpsMethodType::fromString);
  }

}

