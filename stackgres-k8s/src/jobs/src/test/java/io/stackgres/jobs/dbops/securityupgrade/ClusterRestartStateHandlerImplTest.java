/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.ClusterStateHandlerTest;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;

@QuarkusTest
class ClusterRestartStateHandlerImplTest extends ClusterStateHandlerTest {

  @Inject
  @StateHandler("securityUpgrade")
  ClusterRestartStateHandlerImpl restartStateHandler;

  @Override
  public AbstractRestartStateHandler getRestartStateHandler() {
    return restartStateHandler;
  }

  @Override
  public DbOpsRestartStatus getRestartStatus(ClusterRestartState clusterRestartState) {
    return clusterRestartState.getDbOps().getStatus().getSecurityUpgrade();
  }

  @Override
  public Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade);
  }

  public void initializeDbOpsStatus(StackGresDbOps dbOps, List<Pod> pods) {
    final StackGresDbOpsSecurityUpgradeStatus securityUpgrade = new StackGresDbOpsSecurityUpgradeStatus();
    securityUpgrade.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .collect(Collectors.toList())
    );
    securityUpgrade.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    securityUpgrade.setPendingToRestartInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .collect(Collectors.toList())
    );
    securityUpgrade.setSwitchoverInitiated(Boolean.FALSE.toString());

    dbOps.getStatus().setSecurityUpgrade(securityUpgrade);
  }

  @Override
  protected void initializeClusterStatus(StackGresCluster cluster, List<Pod> pods) {

    final StackGresClusterStatus status = new StackGresClusterStatus();
    final StackGresClusterDbOpsStatus dbOps = new StackGresClusterDbOpsStatus();
    final StackGresClusterDbOpsSecurityUpgradeStatus securityUpgrade = new StackGresClusterDbOpsSecurityUpgradeStatus();
    securityUpgrade.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .collect(Collectors.toList())
    );
    securityUpgrade.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    dbOps.setSecurityUpgrade(securityUpgrade);
    status.setDbOps(dbOps);
    cluster.setStatus(status);

  }
}