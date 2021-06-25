/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.jobs.dbops.ClusterRestartStateHandler;
import io.stackgres.jobs.dbops.DatabaseOperation;
import io.stackgres.jobs.dbops.DatabaseOperationJob;
import io.stackgres.jobs.dbops.StateHandler;

@ApplicationScoped
@DatabaseOperation("minorVersionUpgrade")
public class MinorVersionUpgradeJob implements DatabaseOperationJob {

  private final ClusterRestartStateHandler restartStateHandler;

  @Inject
  public MinorVersionUpgradeJob(
      @StateHandler("minorVersionUpgrade")
      ClusterRestartStateHandler restartStateHandler) {
    this.restartStateHandler = restartStateHandler;
  }

  @Override
  public Uni<StackGresDbOps> runJob(StackGresDbOps dbOps, StackGresCluster cluster) {

    return restartStateHandler.restartCluster(dbOps);
  }

}
