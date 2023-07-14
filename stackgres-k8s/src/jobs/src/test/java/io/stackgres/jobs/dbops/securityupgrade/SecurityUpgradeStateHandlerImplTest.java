/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.ClusterStateHandlerTest;
import io.stackgres.jobs.dbops.StateHandler;
import jakarta.inject.Inject;

@QuarkusTest
class SecurityUpgradeStateHandlerImplTest extends ClusterStateHandlerTest {

  @Inject
  @StateHandler("securityUpgrade")
  SecurityUpgradeStateHandlerImpl restartStateHandler;

  @Override
  public AbstractRestartStateHandler getRestartStateHandler() {
    return restartStateHandler;
  }

  @Override
  protected StackGresDbOps getDbOps() {
    return Fixtures.dbOps().loadSecurityUpgrade().get();
  }

  @Override
  protected DbOpsMethodType getRestartMethod(StackGresDbOps dbOps) {
    return DbOpsMethodType.fromString(dbOps.getSpec().getSecurityUpgrade().getMethod());
  }

  @Override
  public DbOpsRestartStatus getRestartStatus(StackGresDbOps dbOps) {
    return dbOps.getStatus().getSecurityUpgrade();
  }

  @Override
  public Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster dbOps) {
    return Optional.ofNullable(dbOps.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getSecurityUpgrade);
  }

  @Override
  public void initializeDbOpsStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods) {
    final StackGresDbOpsSecurityUpgradeStatus securityUpgrade =
        new StackGresDbOpsSecurityUpgradeStatus();
    securityUpgrade.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    securityUpgrade.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    securityUpgrade.setPendingToRestartInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    securityUpgrade.setSwitchoverInitiated(null);

    dbOps.getStatus().setSecurityUpgrade(securityUpgrade);
  }

  @Override
  protected void initializeClusterStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods) {
    final StackGresClusterStatus status = new StackGresClusterStatus();
    final StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    final StackGresClusterDbOpsSecurityUpgradeStatus securityUpgradeStatus =
        new StackGresClusterDbOpsSecurityUpgradeStatus();
    securityUpgradeStatus.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    securityUpgradeStatus.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    dbOpsStatus.setSecurityUpgrade(securityUpgradeStatus);
    status.setDbOps(dbOpsStatus);
    cluster.setStatus(status);
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterDbOpsRestartStatus(StackGresCluster cluster) {
    return cluster.getStatus().getDbOps().getSecurityUpgrade();
  }

  @Override
  protected DbOpsRestartStatus getDbOpsRestartStatus(StackGresDbOps dbOps) {
    return dbOps.getStatus().getSecurityUpgrade();
  }

}
