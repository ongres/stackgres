/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.ClusterStateHandlerTest;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ImmutablePatroniInformation;
import io.stackgres.jobs.dbops.clusterrestart.MemberRole;
import io.stackgres.jobs.dbops.clusterrestart.MemberState;
import io.stackgres.jobs.dbops.clusterrestart.PatroniApiHandler;
import io.stackgres.jobs.dbops.lock.LockAcquirer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class MinorVersionUpgradeRestartStateHandlerTest extends ClusterStateHandlerTest {

  @InjectMock
  LockAcquirer lockAcquirer;

  @Inject
  @StateHandler("minorVersionUpgrade")
  MinorVersionUpgradeRestartStateHandler restartStateHandler;

  @InjectMock
  PatroniApiHandler patroniApi;

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();
    lenient().when(lockAcquirer.lockRun(any(), any()))
        .then(invocation -> (Uni<?>) invocation.getArguments()[1]);
    lenient().when(patroniApi.getClusterMembersPatroniInformation(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(
            List.of(
                ImmutablePatroniInformation.builder()
                    .state(MemberState.RUNNING)
                    .role(MemberRole.LEADER)
                    .serverVersion(110005)
                    .patroniVersion("1.6.5")
                    .patroniScope(clusterName)
                    .isPendingRestart(false)
                    .build(),
                ImmutablePatroniInformation.builder()
                    .state(MemberState.RUNNING)
                    .role(MemberRole.REPLICA)
                    .serverVersion(110005)
                    .patroniVersion("1.6.5")
                    .patroniScope(clusterName)
                    .isPendingRestart(false)
                    .build())));
  }

  @Override
  public AbstractRestartStateHandler getRestartStateHandler() {
    return restartStateHandler;
  }

  @Override
  protected StackGresDbOps getDbOps() {
    return Fixtures.dbOps().loadMinorVersionUpgrade().get();
  }

  @Override
  protected DbOpsMethodType getRestartMethod(StackGresDbOps dbOps) {
    return DbOpsMethodType.fromString(dbOps.getSpec().getMinorVersionUpgrade().getMethod());
  }

  @Override
  public DbOpsRestartStatus getRestartStatus(StackGresDbOps dbOps) {
    return dbOps.getStatus().getMinorVersionUpgrade();
  }

  @Override
  public Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade);
  }

  @Override
  protected void initializeDbOpsStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods) {
    final StackGresDbOpsMinorVersionUpgradeStatus minorVersionUpgradeStatus =
        new StackGresDbOpsMinorVersionUpgradeStatus();
    minorVersionUpgradeStatus.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    minorVersionUpgradeStatus.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    minorVersionUpgradeStatus.setPendingToRestartInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    minorVersionUpgradeStatus.setSwitchoverInitiated(null);
    minorVersionUpgradeStatus.setSourcePostgresVersion(
        cluster.getSpec().getPostgres().getVersion());
    minorVersionUpgradeStatus.setTargetPostgresVersion(
        dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion());

    dbOps.getStatus().setMinorVersionUpgrade(minorVersionUpgradeStatus);
  }

  @Override
  protected void initializeClusterStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods) {
    final StackGresClusterStatus status = new StackGresClusterStatus();
    final StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    final StackGresClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgradeStatus =
        new StackGresClusterDbOpsMinorVersionUpgradeStatus();
    minorVersionUpgradeStatus.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .toList());
    minorVersionUpgradeStatus.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    minorVersionUpgradeStatus.setSourcePostgresVersion(
        cluster.getSpec().getPostgres().getVersion());
    minorVersionUpgradeStatus.setTargetPostgresVersion(
        dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion());
    dbOpsStatus.setMinorVersionUpgrade(minorVersionUpgradeStatus);
    status.setDbOps(dbOpsStatus);
    cluster.setStatus(status);
  }

  @Override
  protected ClusterDbOpsRestartStatus getClusterDbOpsRestartStatus(StackGresCluster cluster) {
    return cluster.getStatus().getDbOps().getMinorVersionUpgrade();
  }

  @Override
  protected DbOpsRestartStatus getDbOpsRestartStatus(StackGresDbOps dbOps) {
    return dbOps.getStatus().getMinorVersionUpgrade();
  }

  @Override
  protected void verifyClusterInitializedStatus(List<Pod> pods, StackGresDbOps dbOps,
      StackGresCluster cluster) {
    super.verifyClusterInitializedStatus(pods, dbOps, cluster);
    var restartStatus = cluster.getStatus().getDbOps().getMinorVersionUpgrade();
    assertEquals(dbOps.getStatus().getMinorVersionUpgrade().getTargetPostgresVersion(),
        restartStatus.getTargetPostgresVersion());
    assertEquals(dbOps.getStatus().getMinorVersionUpgrade().getSourcePostgresVersion(),
        cluster.getStatus().getDbOps().getMinorVersionUpgrade().getSourcePostgresVersion());
  }
}
