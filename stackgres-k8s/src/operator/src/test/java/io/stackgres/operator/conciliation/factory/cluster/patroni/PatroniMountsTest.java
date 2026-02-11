/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniMountsTest {

  @Mock
  private ClusterContainerContext containerContext;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private StackGresClusterContext clusterContext;

  private PatroniMounts patroniMounts;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    patroniMounts = new PatroniMounts();
    cluster = Fixtures.cluster().loadDefault().get();
    when(containerContext.getClusterContext()).thenReturn(clusterContext);
    lenient().when(clusterContext.getSource()).thenReturn(cluster);
    lenient().when(clusterContext.getCluster()).thenReturn(cluster);
  }

  @Test
  void getVolumeMounts_shouldReturnThreeMounts() {
    List<VolumeMount> volumeMounts = patroniMounts.getVolumeMounts(containerContext);

    assertEquals(3, volumeMounts.size());
  }

  @Test
  void getVolumeMounts_shouldContainPatroniEnvMount() {
    List<VolumeMount> volumeMounts = patroniMounts.getVolumeMounts(containerContext);

    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> StackGresVolume.PATRONI_ENV.getName().equals(vm.getName())));
  }

  @Test
  void getVolumeMounts_shouldContainPatroniCredentialsMount() {
    List<VolumeMount> volumeMounts = patroniMounts.getVolumeMounts(containerContext);

    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> StackGresVolume.PATRONI_CREDENTIALS.getName().equals(vm.getName())));
  }

  @Test
  void getVolumeMounts_shouldContainPatroniConfigMount() {
    List<VolumeMount> volumeMounts = patroniMounts.getVolumeMounts(containerContext);

    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> StackGresVolume.PATRONI_CONFIG.getName().equals(vm.getName())));
  }

  @Test
  void getVolumeMounts_patroniConfigShouldMountToConfigPath() {
    List<VolumeMount> volumeMounts = patroniMounts.getVolumeMounts(containerContext);

    VolumeMount configMount = volumeMounts.stream()
        .filter(vm -> StackGresVolume.PATRONI_CONFIG.getName().equals(vm.getName()))
        .findFirst()
        .orElseThrow();

    assertEquals(ClusterPath.PATRONI_CONFIG_PATH.path(), configMount.getMountPath());
  }

  @Test
  void getDerivedEnvVars_shouldReturnEnvVars() {
    List<EnvVar> envVars = patroniMounts.getDerivedEnvVars(containerContext);

    assertFalse(envVars.isEmpty());
  }

  @Test
  void getDerivedEnvVars_shouldReturnFiveEnvVars() {
    List<EnvVar> envVars = patroniMounts.getDerivedEnvVars(containerContext);

    assertEquals(5, envVars.size());
  }

  @Test
  void getDerivedEnvVars_shouldContainPatroniEnvPathEnvVar() {
    List<EnvVar> envVars = patroniMounts.getDerivedEnvVars(containerContext);

    assertTrue(envVars.stream()
        .anyMatch(env -> "PATRONI_ENV_PATH".equals(env.getName())));
  }

  @Test
  void getDerivedEnvVars_shouldContainPatroniConfigPathEnvVar() {
    List<EnvVar> envVars = patroniMounts.getDerivedEnvVars(containerContext);

    assertTrue(envVars.stream()
        .anyMatch(env -> "PATRONI_CONFIG_PATH".equals(env.getName())));
  }

  @Test
  void getDerivedEnvVars_shouldContainPatroniConfigFilePathEnvVar() {
    List<EnvVar> envVars = patroniMounts.getDerivedEnvVars(containerContext);

    assertTrue(envVars.stream()
        .anyMatch(env -> "PATRONI_CONFIG_FILE_PATH".equals(env.getName())));
  }
}
