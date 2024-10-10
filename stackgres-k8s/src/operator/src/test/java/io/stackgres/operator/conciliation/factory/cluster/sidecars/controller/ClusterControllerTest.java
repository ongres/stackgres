/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresModules;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolumeIoLimitsBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.CgroupMounts;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterControllerTest {

  @Mock
  private PostgresDataMounts postgresDataMounts;

  @Mock
  private UserOverrideMounts userOverrideMounts;

  @Mock
  private PostgresSocketMounts postgresSocketMounts;

  @Mock
  private CgroupMounts cgroupMounts;

  @Mock
  private OperatorInstallationInfoHolder installationInfoHolder;

  private ClusterController clusterController;

  @BeforeEach
  void setUp() {
    clusterController = new ClusterController(
        postgresDataMounts, userOverrideMounts, postgresSocketMounts, cgroupMounts, installationInfoHolder);
  }

  @Test
  void getContainer_shouldCreateContainerWithCorrectImage() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = clusterController.getContainer(context);

    Assertions.assertEquals(
        StackGresContainer.CLUSTER_CONTROLLER.getName(), container.getName());
    Assertions.assertEquals(
        StackGresModules.CLUSTER_CONTROLLER.getImageName(), container.getImage());
  }

  @Test
  void getContainer_whenConnectionPoolingEnabled_shouldReconcilePgBouncer() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods()
        .setDisableConnectionPooling(false);

    Container container = clusterController.getContainer(context);

    String reconcilePgBouncerValue = container.getEnv().stream()
        .filter(env -> env.getName().equals(
            ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER
                .getEnvironmentVariableName()))
        .findFirst()
        .orElseThrow()
        .getValue();
    Assertions.assertEquals(Boolean.TRUE.toString(), reconcilePgBouncerValue);
  }

  @Test
  void getContainer_whenConnectionPoolingDisabled_shouldNotReconcilePgBouncer() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods()
        .setDisableConnectionPooling(true);

    Container container = clusterController.getContainer(context);

    String reconcilePgBouncerValue = container.getEnv().stream()
        .filter(env -> env.getName().equals(
            ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_PGBOUNCER
                .getEnvironmentVariableName()))
        .findFirst()
        .orElseThrow()
        .getValue();
    Assertions.assertEquals(Boolean.FALSE.toString(), reconcilePgBouncerValue);
  }

  @Test
  void getContainer_shouldHaveCorrectVolumeMounts() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = clusterController.getContainer(context);

    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.PGBOUNCER_CONFIG.getName())),
        "Should contain pgbouncer config volume mount");
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.PATRONI_CONFIG.getName())),
        "Should contain patroni config volume mount");
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.POSTGRES_SSL.getName())),
        "Should contain postgres ssl volume mount");
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.POSTGRES_SSL_COPY.getName())),
        "Should contain postgres ssl copy volume mount");
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .anyMatch(vm -> vm.getMountPath().equals(ClusterPathV1.PGBOUNCER_CONFIG_PATH.path())),
        "Should contain pgbouncer config mount path");
    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .anyMatch(vm -> vm.getMountPath().equals(ClusterPathV1.PATRONI_CONFIG_PATH.path())),
        "Should contain patroni config mount path");
  }

  @Test
  void getContainer_whenIoLimitsSet_shouldSetApplyIoLimitsEnvVarToTrue() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods()
        .getPersistentVolume().setIoLimits(
            new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
            .withReadIops(100)
            .build());

    Container container = clusterController.getContainer(context);

    String applyIoLimitsValue = container.getEnv().stream()
        .filter(env -> env.getName().equals(
            ClusterControllerProperty.CLUSTER_CONTROLLER_APPLY_IO_LIMITS
                .getEnvironmentVariableName()))
        .findFirst()
        .orElseThrow()
        .getValue();
    Assertions.assertEquals(Boolean.TRUE.toString(), applyIoLimitsValue);
  }

  @Test
  void getContainer_whenNoIoLimitsSet_shouldSetApplyIoLimitsEnvVarToFalse() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = clusterController.getContainer(context);

    String applyIoLimitsValue = container.getEnv().stream()
        .filter(env -> env.getName().equals(
            ClusterControllerProperty.CLUSTER_CONTROLLER_APPLY_IO_LIMITS
                .getEnvironmentVariableName()))
        .findFirst()
        .orElseThrow()
        .getValue();
    Assertions.assertEquals(Boolean.FALSE.toString(), applyIoLimitsValue);
  }

  @Test
  void getContainer_whenIoLimitsSet_shouldIncludeCgroupVolumeMounts() {
    when(cgroupMounts.getVolumeMounts(any()))
        .thenReturn(List.of(
            new VolumeMountBuilder()
                .withName(StackGresVolume.CGROUP.getName())
                .withMountPath(ClusterPathV1.HOST_CGROUP_PATH.path())
                .build()));
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods()
        .getPersistentVolume().setIoLimits(
            new StackGresClusterPodsPersistentVolumeIoLimitsBuilder()
            .withReadIops(100)
            .build());

    Container container = clusterController.getContainer(context);

    Assertions.assertTrue(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.CGROUP.getName())),
        "Should contain cgroup volume mount when IO limits are set");
  }

  @Test
  void getContainer_whenNoIoLimitsSet_shouldNotIncludeCgroupVolumeMounts() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = clusterController.getContainer(context);

    Assertions.assertFalse(
        container.getVolumeMounts().stream()
            .map(VolumeMount::getName)
            .anyMatch(name -> name.equals(StackGresVolume.CGROUP.getName())),
        "Should not contain cgroup volume mount when IO limits are not set");
  }

  @Test
  void getComponentVersions_shouldReturnClusterControllerVersion() {
    ClusterContainerContext context = getClusterContainerContext();

    Map<String, String> versions = clusterController.getComponentVersions(context);

    Assertions.assertTrue(
        versions.containsKey(StackGresKeys.CLUSTER_CONTROLLER_VERSION_KEY));
    Assertions.assertEquals(
        StackGresModules.CLUSTER_CONTROLLER.getVersion(),
        versions.get(StackGresKeys.CLUSTER_CONTROLLER_VERSION_KEY));
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
        .context(StackGresContextMock.CONTEXT)
            .config(getDefaultConfig())
            .source(getDefaultCluster())
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresInstanceProfile())
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
