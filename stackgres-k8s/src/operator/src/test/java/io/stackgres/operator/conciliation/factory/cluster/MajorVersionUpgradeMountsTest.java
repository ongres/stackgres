/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MajorVersionUpgradeMountsTest {

  private static final String SOURCE_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedVersions(StackGresContextMock.CONTEXT)
      .filter(version -> version.startsWith("15."))
      .findFirst().get();

  private static final String TARGET_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
      .streamOrderedVersions(StackGresContextMock.CONTEXT).get(0).get();

  private MajorVersionUpgradeMounts majorVersionUpgradeMounts;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    majorVersionUpgradeMounts = new MajorVersionUpgradeMounts();
    majorVersionUpgradeMounts.postgresDataMounts = new PostgresDataMounts();
    majorVersionUpgradeMounts.postgresExtensionMounts = new PostgresExtensionMounts();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(TARGET_VERSION);
    cluster.getStatus().setPostgresVersion(TARGET_VERSION);
  }

  @Test
  void volumeMounts_shouldMountThePathsOfThePreviousPostgresVersionOnlyOnce() {
    final List<VolumeMount> volumeMounts =
        majorVersionUpgradeMounts.getVolumeMounts(getClusterContainerContext());

    final List<String> mountPaths = volumeMounts.stream()
        .map(VolumeMount::getMountPath)
        .toList();
    assertEquals(Set.copyOf(mountPaths).size(), mountPaths.size(),
        "Duplicated mount paths in " + mountPaths);
    assertTrue(mountPaths.contains("/postgres/" + SOURCE_VERSION + "/bin"), mountPaths.toString());
    assertTrue(mountPaths.contains("/postgres/" + TARGET_VERSION + "/bin"), mountPaths.toString());
  }

  @Test
  void volumeMounts_shouldMountTheDataVolumeOnTheBasePath() {
    final List<VolumeMount> volumeMounts =
        majorVersionUpgradeMounts.getVolumeMounts(getClusterContainerContext());

    assertTrue(volumeMounts.stream()
        .anyMatch(volumeMount -> "test".equals(volumeMount.getName())
            && "/var/db/postgresql".equals(volumeMount.getMountPath())
            && volumeMount.getSubPath() == null),
        volumeMounts.toString());
  }

  @Test
  void volumeMounts_shouldMountTheDataVolumeOnTheBasePathOnRollback() {
    setRollback();

    final List<VolumeMount> volumeMounts =
        majorVersionUpgradeMounts.getVolumeMounts(getClusterContainerContext());

    assertTrue(volumeMounts.stream()
        .anyMatch(volumeMount -> "test".equals(volumeMount.getName())
            && "/var/db/postgresql".equals(volumeMount.getMountPath())
            && volumeMount.getSubPath() == null),
        volumeMounts.toString());
  }

  @Test
  void derivedEnvVars_shouldIncludeThePathsOfTheData() {
    final List<EnvVar> envVars =
        majorVersionUpgradeMounts.getDerivedEnvVars(getClusterContainerContext());

    assertEnvVar(envVars, "PG_BASE_PATH", "/var/db/postgresql");
    assertEnvVar(envVars, "PG_DATA_PATH", "/var/db/postgresql/data");
  }

  @Test
  void derivedEnvVars_shouldUseThePreviousPostgresVersionForTheSourcePaths() {
    final List<EnvVar> envVars =
        majorVersionUpgradeMounts.getDerivedEnvVars(getClusterContainerContext());

    assertEnvVar(envVars, "SOURCE_PG_BIN_PATH", "/postgres/" + SOURCE_VERSION + "/bin");
    assertEnvVar(envVars, "TARGET_PG_BIN_PATH", "/postgres/" + TARGET_VERSION + "/bin");
    assertEnvVar(envVars, "SOURCE_PG_LIB_PATH", "/postgres/" + SOURCE_VERSION + "/lib");
    assertEnvVar(envVars, "TARGET_PG_LIB_PATH", "/postgres/" + TARGET_VERSION + "/lib");
  }

  private void setRollback() {
    final StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgrade =
        new StackGresClusterDbOpsMajorVersionUpgradeStatus();
    majorVersionUpgrade.setRollback(true);
    final StackGresClusterDbOpsStatus dbOps = new StackGresClusterDbOpsStatus();
    dbOps.setMajorVersionUpgrade(majorVersionUpgrade);
    cluster.getStatus().setDbOps(dbOps);
  }

  private void assertEnvVar(List<EnvVar> envVars, String name, String value) {
    assertTrue(envVars.stream()
        .anyMatch(envVar -> name.equals(envVar.getName()) && value.equals(envVar.getValue())),
        "Expected env var " + name + "=" + value + " not found in: " + envVars);
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .context(StackGresContextMock.CONTEXT)
            .config(getDefaultConfig())
            .source(cluster)
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresInstanceProfile())
            .currentInstances(0)
            .build())
        .dataVolumeName("test")
        .oldPostgresVersion(SOURCE_VERSION)
        .build();
  }

  private StackGresConfig getDefaultConfig() {
    return Fixtures.config().loadDefault().get();
  }

}
