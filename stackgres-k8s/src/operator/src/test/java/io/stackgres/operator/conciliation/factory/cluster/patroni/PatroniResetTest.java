/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Container;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.controller.SingleReconciliationCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniResetTest {

  @Mock
  private SingleReconciliationCycle singleReconciliationCycle;

  @Mock
  private ClusterContainerContext containerContext;

  @Mock
  private StackGresClusterContext clusterContext;

  private PatroniReset patroniReset;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    patroniReset = new PatroniReset(singleReconciliationCycle);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(containerContext.getClusterContext()).thenReturn(clusterContext);
    lenient().when(clusterContext.getSource()).thenReturn(cluster);
    lenient().when(clusterContext.getCluster()).thenReturn(cluster);
  }

  @Test
  void isActivated_whenMajorVersionUpgradeStatusPresentAndCheckNotTrue_shouldReturnTrue() {
    StackGresClusterStatus status = new StackGresClusterStatus();
    StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    StackGresClusterDbOpsMajorVersionUpgradeStatus upgradeStatus =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    upgradeStatus.setCheck(false);
    dbOpsStatus.setMajorVersionUpgrade(upgradeStatus);
    status.setDbOps(dbOpsStatus);
    cluster.setStatus(status);

    assertTrue(patroniReset.isActivated(containerContext));
  }

  @Test
  void isActivated_whenMajorVersionUpgradeStatusPresentAndCheckTrue_shouldReturnFalse() {
    StackGresClusterStatus status = new StackGresClusterStatus();
    StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    StackGresClusterDbOpsMajorVersionUpgradeStatus upgradeStatus =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    upgradeStatus.setCheck(true);
    dbOpsStatus.setMajorVersionUpgrade(upgradeStatus);
    status.setDbOps(dbOpsStatus);
    cluster.setStatus(status);

    assertFalse(patroniReset.isActivated(containerContext));
  }

  @Test
  void isActivated_whenNoMajorVersionUpgradeStatus_shouldReturnFalse() {
    cluster.setStatus(new StackGresClusterStatus());

    assertFalse(patroniReset.isActivated(containerContext));
  }

  @Test
  void isActivated_whenNoStatus_shouldReturnFalse() {
    cluster.setStatus(null);

    assertFalse(patroniReset.isActivated(containerContext));
  }

  @Test
  void getContainer_shouldHaveCorrectName() {
    Container baseContainer = new Container();
    baseContainer.setName("base-container");
    when(singleReconciliationCycle.getContainer(containerContext)).thenReturn(baseContainer);

    Container container = patroniReset.getContainer(containerContext);

    assertEquals(StackGresInitContainer.RESET_PATRONI.getName(), container.getName());
  }

  @Test
  void getContainer_shouldContainReconcilePatroniEnvVar() {
    Container baseContainer = new Container();
    baseContainer.setName("base-container");
    when(singleReconciliationCycle.getContainer(containerContext)).thenReturn(baseContainer);

    Container container = patroniReset.getContainer(containerContext);

    assertNotNull(container.getEnv());
    assertTrue(container.getEnv().stream()
        .anyMatch(env ->
            ClusterControllerProperty
                .CLUSTER_CONTROLLER_RECONCILE_PATRONI_AFTER_MAJOR_VERSION_UPGRADE
                .getEnvironmentVariableName()
                .equals(env.getName())
                && Boolean.TRUE.toString().equals(env.getValue())));
  }

  @Test
  void getComponentVersions_shouldReturnEmptyMap() {
    assertTrue(patroniReset.getComponentVersions(containerContext).isEmpty());
  }
}
