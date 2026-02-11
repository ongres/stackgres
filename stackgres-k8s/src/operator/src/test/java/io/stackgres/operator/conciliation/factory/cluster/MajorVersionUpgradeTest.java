/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.MajorVersionUpgradeMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MajorVersionUpgradeTest {

  @Mock
  private MajorVersionUpgradeMounts majorVersionUpgradeMounts;

  @Mock
  private TemplatesMounts templateMounts;

  private MajorVersionUpgrade majorVersionUpgrade;

  private StackGresCluster cluster;

  private static final String SOURCE_VERSION = "15.2";
  private static final String TARGET_VERSION;

  static {
    TARGET_VERSION = StackGresComponent.POSTGRESQL.getLatest()
        .streamOrderedVersions().get(0).get();
  }

  @BeforeEach
  void setUp() {
    majorVersionUpgrade = new MajorVersionUpgrade(majorVersionUpgradeMounts, templateMounts);
    cluster = Fixtures.cluster().loadDefault().get();

    lenient().when(majorVersionUpgradeMounts.getDerivedEnvVars(any()))
        .thenReturn(List.of());
    lenient().when(majorVersionUpgradeMounts.getVolumeMounts(any()))
        .thenReturn(List.of());
    lenient().when(templateMounts.getVolumeMounts(any()))
        .thenReturn(List.of());
  }

  @Test
  void isActivated_whenMajorVersionUpgradePresent_shouldBeActivated() {
    setupMajorVersionUpgradeStatus(SOURCE_VERSION, TARGET_VERSION);
    ClusterContainerContext context = getClusterContainerContext();

    assertTrue(majorVersionUpgrade.isActivated(context));
  }

  @Test
  void isActivated_whenNoUpgradeStatus_shouldNotBeActivated() {
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setPostgresVersion(TARGET_VERSION);
    ClusterContainerContext context = getClusterContainerContext();

    assertFalse(majorVersionUpgrade.isActivated(context));
  }

  @Test
  void isActivated_whenDbOpsStatusPresentButNoMajorVersionUpgrade_shouldNotBeActivated() {
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setPostgresVersion(TARGET_VERSION);
    cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
    ClusterContainerContext context = getClusterContainerContext();

    assertFalse(majorVersionUpgrade.isActivated(context));
  }

  @Test
  void isActivated_whenSourceVersionEqualsCurrent_shouldNotBeActivated() {
    String currentVersion = TARGET_VERSION;
    setupMajorVersionUpgradeStatus(currentVersion, TARGET_VERSION);
    ClusterContainerContext context = getClusterContainerContext();

    assertFalse(majorVersionUpgrade.isActivated(context));
  }

  @Test
  void isActivated_whenRollbackIsTrue_shouldBeActivated() {
    setupMajorVersionUpgradeStatus(TARGET_VERSION, TARGET_VERSION);
    cluster.getStatus().getDbOps().getMajorVersionUpgrade().setRollback(true);
    ClusterContainerContext context = getClusterContainerContext();

    assertTrue(majorVersionUpgrade.isActivated(context));
  }

  @Test
  void getContainer_shouldHaveVersionSpecificEnvVars() {
    setupMajorVersionUpgradeStatus(SOURCE_VERSION, TARGET_VERSION);
    ClusterContainerContext context = getClusterContainerContext();

    Container container = majorVersionUpgrade.getContainer(context);

    assertEquals(StackGresInitContainer.MAJOR_VERSION_UPGRADE.getName(), container.getName());

    List<EnvVar> envVars = container.getEnv();
    assertEnvVarPresent(envVars, "TARGET_VERSION", TARGET_VERSION);
    assertEnvVarPresent(envVars, "SOURCE_VERSION", SOURCE_VERSION);
    assertEnvVarPresent(envVars, "POSTGRES_VERSION", TARGET_VERSION);
    assertEnvVarPresent(envVars, "PRIMARY_INSTANCE", "test-instance");
    assertEnvVarPresent(envVars, "LOCALE", "en_US.UTF-8");
    assertEnvVarPresent(envVars, "ENCODING", "UTF8");
    assertEnvVarPresent(envVars, "DATA_CHECKSUM", "true");
    assertEnvVarPresent(envVars, "LINK", "false");
    assertEnvVarPresent(envVars, "CLONE", "false");
    assertEnvVarPresent(envVars, "CHECK", "true");
  }

  private void setupMajorVersionUpgradeStatus(String sourceVersion, String targetVersion) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    cluster.getStatus().setPostgresVersion(targetVersion);
    if (cluster.getStatus().getDbOps() == null) {
      cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus upgradeStatus =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    upgradeStatus.setSourcePostgresVersion(sourceVersion);
    upgradeStatus.setTargetPostgresVersion(targetVersion);
    upgradeStatus.setPrimaryInstance("test-instance");
    upgradeStatus.setLocale("en_US.UTF-8");
    upgradeStatus.setEncoding("UTF8");
    upgradeStatus.setDataChecksum(true);
    upgradeStatus.setLink(false);
    upgradeStatus.setClone(false);
    upgradeStatus.setCheck(true);
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(upgradeStatus);
  }

  private void assertEnvVarPresent(List<EnvVar> envVars, String name, String value) {
    assertTrue(envVars.stream()
        .anyMatch(env -> name.equals(env.getName()) && value.equals(env.getValue())),
        "Expected env var " + name + "=" + value + " not found in: " + envVars);
  }

  private ClusterContainerContext getClusterContainerContext() {
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

}
