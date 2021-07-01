/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.minorversionupgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.jobs.dbops.AbstractRestartStateHandler;
import io.stackgres.jobs.dbops.ClusterStateHandlerTest;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutablePatroniInformation;
import io.stackgres.jobs.dbops.clusterrestart.MemberRole;
import io.stackgres.jobs.dbops.clusterrestart.MemberState;
import io.stackgres.jobs.dbops.clusterrestart.PatroniApiHandler;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
class MinorVersionUpgradeRestartStateHandlerImplTest extends ClusterStateHandlerTest {

  @Inject
  @StateHandler("minorVersionUpgrade")
  MinorVersionUpgradeRestartStateHandlerImpl restartStateHandler;

  @InjectMock
  PatroniApiHandler patroniApi;

  @Override
  @BeforeEach
  public void setUp() {
    super.setUp();
    lenient().when(patroniApi.getMembersPatroniInformation(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(
            List.of(
                ImmutablePatroniInformation.builder()
                    .state(MemberState.RUNNING)
                    .role(MemberRole.LEADER)
                    .serverVersion(110005)
                    .patroniVersion("1.6.5")
                    .patroniScope(clusterName)
                .build(),
                ImmutablePatroniInformation.builder()
                    .state(MemberState.RUNNING)
                    .role(MemberRole.REPlICA)
                    .serverVersion(110005)
                    .patroniVersion("1.6.5")
                    .patroniScope(clusterName)
                    .build()
            )
        ));
  }

  @Override
  public AbstractRestartStateHandler getRestartStateHandler() {
    return restartStateHandler;
  }

  @Override
  public DbOpsRestartStatus getRestartStatus(ClusterRestartState clusterRestartState) {
    return clusterRestartState.getDbOps().getStatus().getMinorVersionUpgrade();
  }

  @Override
  public Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade);
  }

  @Override
  protected void initializeDbOpsStatus(StackGresDbOps dbOps, List<Pod> pods) {
    final StackGresDbOpsMinorVersionUpgradeStatus minorVersionUpgradeStatus =
        new StackGresDbOpsMinorVersionUpgradeStatus();
    minorVersionUpgradeStatus.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .collect(Collectors.toList())
    );
    minorVersionUpgradeStatus.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    minorVersionUpgradeStatus.setPendingToRestartInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .collect(Collectors.toList())
    );
    minorVersionUpgradeStatus.setSwitchoverInitiated(Boolean.FALSE.toString());

    dbOps.getStatus().setMinorVersionUpgrade(minorVersionUpgradeStatus);

  }

  @Override
  protected void initializeClusterStatus(StackGresCluster cluster, List<Pod> pods) {

    final StackGresClusterStatus status = new StackGresClusterStatus();
    final StackGresClusterDbOpsStatus dbOps = new StackGresClusterDbOpsStatus();
    final StackGresClusterDbOpsMinorVersionUpgradeStatus minorVersionUpgrade =
        new StackGresClusterDbOpsMinorVersionUpgradeStatus();
    minorVersionUpgrade.setInitialInstances(
        pods.stream()
            .map(Pod::getMetadata).map(ObjectMeta::getName)
            .collect(Collectors.toList())
    );
    minorVersionUpgrade.setPrimaryInstance(getPrimaryInstance(pods).getMetadata().getName());
    minorVersionUpgrade.setSourcePostgresVersion("11.6");
    minorVersionUpgrade.setTargetPostgresVersion(cluster.getSpec().getPostgresVersion());
    dbOps.setMinorVersionUpgrade(minorVersionUpgrade);
    status.setDbOps(dbOps);
    cluster.setStatus(status);

  }

  @Override
  protected void verifyClusterInitializedStatus(List<Pod> pods, ClusterRestartState clusterRestartState) {
    super.verifyClusterInitializedStatus(pods, clusterRestartState);
    var restartStatus = clusterRestartState.getCluster().getStatus().getDbOps().getMinorVersionUpgrade();
    assertEquals(cluster.getSpec().getPostgresVersion(), restartStatus.getTargetPostgresVersion());
    assertEquals("11.5", clusterRestartState.getCluster().getStatus().getDbOps().getMinorVersionUpgrade().getSourcePostgresVersion());
  }
}