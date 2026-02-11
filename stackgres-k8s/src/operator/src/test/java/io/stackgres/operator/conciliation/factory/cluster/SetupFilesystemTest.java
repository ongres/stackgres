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
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetupFilesystemTest {

  @Mock
  private PostgresExtensionMounts postgresExtensionsMounts;

  @Mock
  private TemplatesMounts templateMounts;

  @Mock
  private UserOverrideMounts containerUserOverrideMounts;

  private SetupFilesystem setupFilesystem;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    setupFilesystem = new SetupFilesystem(
        postgresExtensionsMounts, templateMounts, containerUserOverrideMounts);
    cluster = Fixtures.cluster().loadDefault().get();

    lenient().when(postgresExtensionsMounts.getDerivedEnvVars(any()))
        .thenReturn(List.of());
    lenient().when(templateMounts.getDerivedEnvVars(any()))
        .thenReturn(List.of());
    lenient().when(templateMounts.getVolumeMounts(any()))
        .thenReturn(List.of());
  }

  @Test
  void getContainer_shouldAlwaysCreateInitContainer() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupFilesystem.getContainer(context);

    assertNotNull(container);
    assertEquals(StackGresInitContainer.SETUP_FILESYSTEM.getName(), container.getName());
  }

  @Test
  void getContainer_shouldHaveCorrectCommand() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupFilesystem.getContainer(context);

    assertEquals(3, container.getCommand().size());
    assertEquals("/bin/sh", container.getCommand().get(0));
    assertEquals("-ex", container.getCommand().get(1));
    assertTrue(container.getCommand().get(2).contains(
        ClusterPath.LOCAL_BIN_SETUP_FILESYSTEM_SH_PATH.filename()));
  }

  @Test
  void getContainer_shouldHaveDataVolumeMount() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupFilesystem.getContainer(context);

    List<VolumeMount> volumeMounts = container.getVolumeMounts();
    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> "test".equals(vm.getName())
            && ClusterPath.PG_BASE_PATH.path().equals(vm.getMountPath())));
  }

  @Test
  void getContainer_shouldHaveUserVolumeMount() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupFilesystem.getContainer(context);

    List<VolumeMount> volumeMounts = container.getVolumeMounts();
    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> StackGresVolume.USER.getName().equals(vm.getName())
            && "/local/etc".equals(vm.getMountPath())
            && "etc".equals(vm.getSubPath())));
  }

  @Test
  void getContainer_shouldHaveHomeEnvVar() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupFilesystem.getContainer(context);

    List<EnvVar> envVars = container.getEnv();
    assertTrue(envVars.stream()
        .anyMatch(env -> "HOME".equals(env.getName()) && "/tmp".equals(env.getValue())));
  }

  @Test
  void getContainer_shouldHaveNonEmptyVolumeMounts() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = setupFilesystem.getContainer(context);

    assertFalse(container.getVolumeMounts().isEmpty());
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
