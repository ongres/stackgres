/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.controller;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleReconciliationCycleTest {

  private SingleReconciliationCycle singleReconciliationCycle;

  @BeforeEach
  void setUp() {
    singleReconciliationCycle = new SingleReconciliationCycle();
  }

  @Test
  void isActivated_whenMajorVersionUpgradeWithDifferentVersion_shouldBeActivated() {
    StackGresCluster cluster = getDefaultCluster();
    // The cluster's status postgresVersion is "13.9" from the fixture.
    // Set the majorVersionUpgrade sourcePostgresVersion to a different value
    // so that sourcePostgresVersion != cluster.status.postgresVersion => activated.
    StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    majorVersionUpgrade.setSourcePostgresVersion("12.0");
    majorVersionUpgrade.setInitialInstances(List.of("stackgres-0"));
    majorVersionUpgrade.setPrimaryInstance("stackgres-0");
    dbOpsStatus.setMajorVersionUpgrade(majorVersionUpgrade);
    cluster.getStatus().setDbOps(dbOpsStatus);

    ClusterContainerContext context = buildContext(cluster);

    Assertions.assertTrue(singleReconciliationCycle.isActivated(context));
  }

  @Test
  void isActivated_whenNoMajorVersionUpgrade_shouldNotBeActivated() {
    StackGresCluster cluster = getDefaultCluster();
    // No dbOps set, so no majorVersionUpgrade => not activated.

    ClusterContainerContext context = buildContext(cluster);

    Assertions.assertFalse(singleReconciliationCycle.isActivated(context));
  }

  @Test
  void isActivated_whenMajorVersionUpgradeWithSameVersionButRollback_shouldBeActivated() {
    final StackGresCluster cluster = getDefaultCluster();
    // sourcePostgresVersion equals cluster.status.postgresVersion ("13.9"),
    // but rollback is true => activated.
    final StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    final StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    majorVersionUpgrade.setSourcePostgresVersion("13.9");
    majorVersionUpgrade.setRollback(true);
    majorVersionUpgrade.setInitialInstances(List.of("stackgres-0"));
    majorVersionUpgrade.setPrimaryInstance("stackgres-0");
    dbOpsStatus.setMajorVersionUpgrade(majorVersionUpgrade);
    cluster.getStatus().setDbOps(dbOpsStatus);

    ClusterContainerContext context = buildContext(cluster);

    Assertions.assertTrue(singleReconciliationCycle.isActivated(context));
  }

  @Test
  void isActivated_whenMajorVersionUpgradeWithSameVersionAndNoRollback_shouldNotBeActivated() {
    StackGresCluster cluster = getDefaultCluster();
    // sourcePostgresVersion equals cluster.status.postgresVersion ("13.9"),
    // and rollback is not set (null / defaults to false) => not activated.
    StackGresClusterDbOpsStatus dbOpsStatus = new StackGresClusterDbOpsStatus();
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    majorVersionUpgrade.setSourcePostgresVersion("13.9");
    majorVersionUpgrade.setInitialInstances(List.of("stackgres-0"));
    majorVersionUpgrade.setPrimaryInstance("stackgres-0");
    dbOpsStatus.setMajorVersionUpgrade(majorVersionUpgrade);
    cluster.getStatus().setDbOps(dbOpsStatus);

    ClusterContainerContext context = buildContext(cluster);

    Assertions.assertFalse(singleReconciliationCycle.isActivated(context));
  }

  @Test
  void getContainer_shouldHaveCommandEnvVarRunReconciliationCycle() {
    StackGresCluster cluster = getDefaultCluster();

    ClusterContainerContext context = buildContext(cluster);

    Container container = singleReconciliationCycle.getContainer(context);

    Assertions.assertEquals(
        StackGresInitContainer.CLUSTER_RECONCILIATION_CYCLE.getName(),
        container.getName());

    String commandValue = container.getEnv().stream()
        .filter(env -> "COMMAND".equals(env.getName()))
        .findFirst()
        .orElseThrow()
        .getValue();
    Assertions.assertEquals("run-reconciliation-cycle", commandValue);
  }

  private ClusterContainerContext buildContext(StackGresCluster cluster) {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .config(getDefaultConfig())
            .source(cluster)
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresProfile())
            .currentInstances(0)
            .build())
        .dataVolumeName("test")
        .build();
  }

  private StackGresConfig getDefaultConfig() {
    return Fixtures.config().loadDefault().get();
  }

  private StackGresCluster getDefaultCluster() {
    return Fixtures.cluster().loadDefault().get();
  }

}
