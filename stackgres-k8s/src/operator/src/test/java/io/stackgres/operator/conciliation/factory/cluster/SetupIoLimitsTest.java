/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolumeIoLimitsBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.CgroupMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetupIoLimitsTest {

  @Mock
  private UserOverrideMounts userOverrideMounts;

  @Mock
  private TemplatesMounts templateMounts;

  @Mock
  private CgroupMounts cgroupMounts;

  private SetupIoLimits setupIoLimits;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    setupIoLimits = new SetupIoLimits(
        userOverrideMounts, templateMounts, cgroupMounts);
    cluster = Fixtures.cluster().loadDefault().get();

    lenient().when(userOverrideMounts.getVolumeMounts(any()))
        .thenReturn(List.of());
    lenient().when(templateMounts.getDerivedEnvVars(any()))
        .thenReturn(List.of());
    lenient().when(templateMounts.getVolumeMounts(any()))
        .thenReturn(List.of());
    lenient().when(cgroupMounts.getVolumeMounts(any()))
        .thenReturn(List.of());
  }

  @Test
  void getContainer_shouldCreateInitContainerWhenReadIopsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withReadIops(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    assertNotNull(container);
    assertEquals(StackGresInitContainer.SETUP_IO_LIMITS.getName(), container.getName());
  }

  @Test
  void getContainer_shouldCreateInitContainerWhenWriteIopsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withWriteIops(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    assertNotNull(container);
    assertEquals(StackGresInitContainer.SETUP_IO_LIMITS.getName(), container.getName());
  }

  @Test
  void getContainer_shouldCreateInitContainerWhenReadMbpsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withReadMiBps(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    assertNotNull(container);
    assertEquals(StackGresInitContainer.SETUP_IO_LIMITS.getName(), container.getName());
  }

  @Test
  void getContainer_shouldCreateInitContainerWhenWriteMbpsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withWriteMiBps(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    assertNotNull(container);
    assertEquals(StackGresInitContainer.SETUP_IO_LIMITS.getName(), container.getName());
  }

  @Test
  void getContainer_shouldHaveCorrectCommand() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    assertEquals(3, container.getCommand().size());
    assertEquals("/bin/sh", container.getCommand().get(0));
    assertEquals("-ex", container.getCommand().get(1));
    assertTrue(container.getCommand().get(2).contains(
        ClusterPath.LOCAL_BIN_SETUP_IO_LIMITS_SH_PATH.filename()));
  }

  @Test
  void getContainer_shouldHaveCgroupVolumeMount() {
    lenient().when(cgroupMounts.getVolumeMounts(any()))
        .thenReturn(List.of(
            new io.fabric8.kubernetes.api.model.VolumeMountBuilder()
                .withName(StackGresVolume.CGROUP.getName())
                .withMountPath(ClusterPath.HOST_CGROUP_PATH.path())
                .build()));
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    List<VolumeMount> volumeMounts = container.getVolumeMounts();
    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> StackGresVolume.CGROUP.getName().equals(vm.getName())
            && ClusterPath.HOST_CGROUP_PATH.path().equals(vm.getMountPath())));
  }

  @Test
  void isActivated_shouldReturnFalseWhenNoIoLimitsSet() {
    ClusterContainerContext context = getClusterContainerContext();

    assertFalse(setupIoLimits.isActivated(context));
  }

  @Test
  void isActivated_shouldReturnTrueWhenReadIopsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withReadIops(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    assertTrue(setupIoLimits.isActivated(context));
  }

  @Test
  void isActivated_shouldReturnTrueWhenWriteIopsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withWriteIops(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    assertTrue(setupIoLimits.isActivated(context));
  }

  @Test
  void isActivated_shouldReturnTrueWhenReadMiBpsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withReadMiBps(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    assertTrue(setupIoLimits.isActivated(context));
  }

  @Test
  void isActivated_shouldReturnTrueWhenWriteMiBpsIsSet() {
    cluster.getSpec().getPods().getPersistentVolume().setIoLimits(
        new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
        .withWriteMiBps(1)
        .build());
    ClusterContainerContext context = getClusterContainerContext();

    assertTrue(setupIoLimits.isActivated(context));
  }

  @Test
  void getContainer_shouldHaveHomeEnvVar() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupIoLimits.getContainer(context);

    List<EnvVar> envVars = container.getEnv();
    assertTrue(envVars.stream()
        .anyMatch(env -> "HOME".equals(env.getName()) && "/tmp".equals(env.getValue())));
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
