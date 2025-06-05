/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestartStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.ClusterStateHandlerTest;
import io.stackgres.jobs.dbops.StateHandler;
import jakarta.inject.Inject;

@WithKubernetesTestServer
@QuarkusTest
class ClusterRestartStateHandlerTest extends ClusterStateHandlerTest {

  @Inject
  @StateHandler("restart")
  ClusterRestartStateHandler restartStateHandler;

  @Override
  public AbstractRestartStateHandler getRestartStateHandler() {
    return restartStateHandler;
  }

  @Override
  protected StackGresDbOps getDbOps() {
    return Fixtures.dbOps().loadRestart().get();
  }

  @Override
  protected DbOpsMethodType getRestartMethod(StackGresDbOps dbOps) {
    return DbOpsMethodType.fromString(dbOps.getSpec().getRestart().getMethod());
  }

  @Override
  public DbOpsRestartStatus getRestartStatus(StackGresDbOps dbOps) {
    return dbOps.getStatus().getRestart();
  }

  @Override
  public Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster dbOps) {
    return Optional.ofNullable(dbOps)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getRestart);
  }

  @Override
  protected void initializeDbOpsStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods) {
    final StackGresDbOpsRestartStatus restartStatus = new StackGresDbOpsRestartStatus();
    restartStatus.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    restartStatus.setPrimaryInstance(getPrimaryInstance(cluster, pods).getMetadata().getName());
    restartStatus.setPendingToRestartInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    restartStatus.setSwitchoverInitiated(Boolean.FALSE.toString());

    dbOps.getStatus().setRestart(restartStatus);
  }

  @Override
  protected void initializeClusterStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods) {
    final StackGresClusterStatus status = new StackGresClusterStatus();
    final StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    final StackGresClusterDbOpsRestartStatus restartStatus =
        new StackGresClusterDbOpsRestartStatus();
    restartStatus.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .limit(2)
            .toList());
    restartStatus.setPrimaryInstance(getPrimaryInstance(cluster, pods).getMetadata().getName());
    dbOpsStatus.setRestart(restartStatus);
    status.setDbOps(dbOpsStatus);
    cluster.setStatus(status);
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterDbOpsRestartStatus(StackGresCluster cluster) {
    return cluster.getStatus().getDbOps().getRestart();
  }

  @Override
  protected DbOpsRestartStatus getDbOpsRestartStatus(StackGresDbOps dbOps) {
    return dbOps.getStatus().getRestart();
  }

}
